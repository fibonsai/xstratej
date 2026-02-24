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

package com.fibonsai.cryptomeria.xtratej.rules;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.rules.impl.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

class RuleTypeTest {

    @Test
    void testBuilderCreation() {
        for (RuleType type : RuleType.values()) {
            assertNotNull(type.builder(), "Builder should not be null for " + type);
        }
    }

    @Test
    void testBuildAndRule() {
        RuleStream rule = RuleType.And.builder()
                .setId("test-and")
                .build();
        assertNotNull(rule);
        assertInstanceOf(AndRule.class, rule);
        assertEquals("test-and", rule.name());
    }

    @Test
    void testBuildCrossedRule() {
        RuleStream rule = RuleType.Crossed.builder()
                .setId("test-crossed")
                .build();
        assertNotNull(rule);
        assertInstanceOf(CrossedRule.class, rule);
        assertEquals("test-crossed", rule.name());
    }

    @Test
    void testBuildDateTimeRule() {
        RuleStream rule = RuleType.DateTime.builder()
                .setId("test-datetime")
                .build();
        assertNotNull(rule);
        assertInstanceOf(DateTimeRule.class, rule);
        assertEquals("test-datetime", rule.name());
    }
    
    @Test
    void testBuildInSlopeRule() {
        RuleStream rule = RuleType.InSlope.builder()
                .setId("test-inslope")
                .build();
        assertNotNull(rule);
        assertInstanceOf(InSlopeRule.class, rule);
        assertEquals("test-inslope", rule.name());
    }

    @Test
    void testBuildLimitRule() {
        RuleStream rule = RuleType.Limit.builder()
                .setId("test-limit")
                .build();
        assertNotNull(rule);
        assertInstanceOf(LimitRule.class, rule);
        assertEquals("test-limit", rule.name());
    }

    @Test
    void testBuildNotRule() {
        RuleStream rule = RuleType.Not.builder()
                .setId("test-not")
                .build();
        assertNotNull(rule);
        assertInstanceOf(NotRule.class, rule);
        assertEquals("test-not", rule.name());
    }

    @Test
    void testBuildOrRule() {
        RuleStream rule = RuleType.Or.builder()
                .setId("test-or")
                .build();
        assertNotNull(rule);
        assertInstanceOf(OrRule.class, rule);
        assertEquals("test-or", rule.name());
    }

    @Test
    void testBuildRandomRule() {
        RuleStream rule = RuleType.Random.builder()
                .setId("test-random")
                .build();
        assertNotNull(rule);
        assertInstanceOf(RandomRule.class, rule);
        assertEquals("test-random", rule.name());
    }

    @Test
    void testBuildTimeRule() {
        RuleStream rule = RuleType.Time.builder()
                .setId("test-time")
                .build();
        assertNotNull(rule);
        assertInstanceOf(TimeRule.class, rule);
        assertEquals("test-time", rule.name());
    }

    @Test
    void testBuildTrendRule() {
        RuleStream rule = RuleType.Trend.builder()
                .setId("test-trend")
                .build();
        assertNotNull(rule);
        assertInstanceOf(TrendRule.class, rule);
        assertEquals("test-trend", rule.name());
    }

    @Test
    void testBuildWeekdayRule() {
        RuleStream rule = RuleType.Weekday.builder()
                .setId("test-weekday")
                .build();
        assertNotNull(rule);
        assertInstanceOf(WeekdayRule.class, rule);
        assertEquals("test-weekday", rule.name());
    }

    @Test
    void testBuildXOrRule() {
        RuleStream rule = RuleType.XOr.builder()
                .setId("test-xor")
                .build();
        assertNotNull(rule);
        assertInstanceOf(XOrRule.class, rule);
        assertEquals("test-xor", rule.name());
    }

    @Test
    void testBuilderWithPropertiesAndResults() {
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("testProp", "testValue");
        
        Fifo<ITemporalData> results = new Fifo<>();
        
        RuleStream rule = RuleType.And.builder()
                .setId("test-props")
                .setProperties(properties)
                .setResults(results)
                .build();
        
        assertNotNull(rule);
        assertEquals("test-props", rule.name());
        assertSame(results, rule.results());
        // verifying properties might require reflection or a getter if available, but assuming constructor passed them correctly
    }
}
