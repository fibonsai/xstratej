/*
 *  Copyright (c) 2026 fibonsai.com
 *  All rights reserved.
 *
 *  This source is subject to the Apache License, Version 2.0.
 *  Please see the LICENSE file for more information.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.fibonsai.xtratej.adapter.duckdb;

import com.fibonsai.xtratej.event.series.dao.Double2TimeSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class DuckDBSubscriberTest {

    private static final Logger log = LoggerFactory.getLogger(DuckDBSubscriberTest.class);

    private static final String S3URL = "s3://my-bucket/instrument.parquet";

    private DuckDBSubscriber subscriber;

    @Container
    private static final MinIOContainer container = new MinIOContainer("minio/minio:RELEASE.2025-09-07T16-13-09Z");

    @BeforeAll
    public static void init() {
        String uploaded = uploadFileToMinIO();
        assertFalse(uploaded.isBlank());
        log.info(uploaded);
    }

    @BeforeEach
    public void beforeEach() {
        String query = "SELECT * FROM read_parquet('%s')".formatted(S3URL);

        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode otherProperties = jsonNodeFactory.objectNode();
        otherProperties.put("s3_use_ssl", false);
        URI endpointUri = URI.create(container.getS3URL());
        otherProperties.put("s3_endpoint", "%s:%s".formatted(endpointUri.getHost(), endpointUri.getPort()));
        otherProperties.put("s3_url_style", "path");
        otherProperties.put("s3_region", "us-east-1");

        ObjectNode newParams = jsonNodeFactory.objectNode();
        newParams.put(DuckDBClient.DuckDBKey.ACCOUNT.key(), container.getUserName());
        newParams.put(DuckDBClient.DuckDBKey.SECRET.key(), container.getPassword());
        newParams.put(DuckDBClient.DuckDBKey.QUERY.key(), query);
        newParams.set(DuckDBClient.DuckDBKey.OTHER_PROPERTIES.key(), otherProperties);

        subscriber = new DuckDBSubscriber("test", "test");
        subscriber.setParams(newParams);
    }

    @AfterEach
    public void afterEach() {
        if (subscriber != null && subscriber.isConnected()) {
            subscriber.disconnect();
            subscriber = null;
        }
    }

    private static boolean bucketExists(S3Client s3Client, String bucketName) {
        try {
            s3Client.headBucket(request -> request.bucket(bucketName));
            return true;
        }
        catch (NoSuchBucketException exception) {
            return false;
        }
    }

    private static String uploadFileToMinIO() {
        try {
            var resource = DuckDBSubscriberTest.class.getClassLoader().getResource("instrument.parquet");
            String parquetFilePath = Paths.get(Objects.requireNonNull(resource).toURI()).toAbsolutePath().toString();
            String bucketName = "my-bucket";
            AwsCredentials credentials = AwsBasicCredentials.create(container.getUserName(), container.getPassword());
            try (S3Client s3Client = S3Client
                    .builder()
                    .endpointOverride(URI.create(container.getS3URL()))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(true)
                    .build()) {
                if (!bucketExists(s3Client, bucketName)) {
                    s3Client.createBucket(request -> request.bucket(bucketName));
                }
                File file = new File(parquetFilePath);
                s3Client.putObject(request ->
                    request.bucket(bucketName)
                            .key(file.getName())
                            .ifNoneMatch("*")
                , file.toPath());

                ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .build();
                ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

                List<S3Object> contents = listObjectsV2Response.contents();
                S3Object s3Object = contents.getFirst();
                return "%s s3://%s/%s: %s bytes".formatted(s3Object.lastModified(), bucketName, s3Object.key(), s3Object.size());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    @Test
    void testConnectAndSubscribe() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        subscriber.onConnect(latch::countDown);
        subscriber.onSubscribe(latch::countDown);
        subscriber.subscribe(_ -> {});
        boolean connected = subscriber.connect();
        assertTrue(connected, "Should connect successfully");
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testReceiveMessageFromS3() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean received = new AtomicBoolean(false);
        subscriber.subscribe(ts -> {
            received.compareAndSet(false, ts instanceof Double2TimeSeries);
            latch.countDown();
        });
        boolean connected = subscriber.connect();
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(connected, "Should connect successfully");
        assertTrue(received.get(), "Should receive message within timeout");
    }

    @Test
    void testDisconnect() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        subscriber.onConnect(latch::countDown);
        boolean connected = subscriber.connect();
        assertTrue(connected, "Should connect successfully");
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        boolean disconnected = subscriber.disconnect();
        assertTrue(disconnected, "Should disconnect successfully");
    }
}
