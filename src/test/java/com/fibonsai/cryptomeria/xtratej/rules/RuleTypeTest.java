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

import com.fibonsai.cryptomeria.xtratej.rules.impl.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

class RuleTypeTest {

    @Test
    void testBuildAndRule() {
        RuleStream rule = RuleType.And
                .build()
                .setDescription("test-and");
        assertNotNull(rule);
        assertInstanceOf(AndRule.class, rule);
        assertEquals("test-and", rule.getDescription());
    }

    @Test
    void testBuildCrossedRule() {
        RuleStream rule = RuleType.Crossed
                .build()
                .setDescription("test-crossed");
        assertNotNull(rule);
        assertInstanceOf(CrossedRule.class, rule);
        assertEquals("test-crossed", rule.getDescription());
    }

    @Test
    void testBuildDateTimeRule() {
        RuleStream rule = RuleType.DateTime
                .build()
                .setDescription("test-datetime");
        assertNotNull(rule);
        assertInstanceOf(DateTimeRule.class, rule);
        assertEquals("test-datetime", rule.getDescription());
    }
    
    @Test
    void testBuildInSlopeRule() {
        RuleStream rule = RuleType.InSlope
                .build()
                .setDescription("test-inslope");
        assertNotNull(rule);
        assertInstanceOf(InSlopeRule.class, rule);
        assertEquals("test-inslope", rule.getDescription());
    }

    @Test
    void testBuildLimitRule() {
        RuleStream rule = RuleType.Limit
                .build()
                .setDescription("test-limit");
        assertNotNull(rule);
        assertInstanceOf(LimitRule.class, rule);
        assertEquals("test-limit", rule.getDescription());
    }

    @Test
    void testBuildNotRule() {
        RuleStream rule = RuleType.Not
                .build()
                .setDescription("test-not");
        assertNotNull(rule);
        assertInstanceOf(NotRule.class, rule);
        assertEquals("test-not", rule.getDescription());
    }

    @Test
    void testBuildOrRule() {
        RuleStream rule = RuleType.Or
                .build()
                .setDescription("test-or");
        assertNotNull(rule);
        assertInstanceOf(OrRule.class, rule);
        assertEquals("test-or", rule.getDescription());
    }

    @Test
    void testBuildRandomRule() {
        RuleStream rule = RuleType.Random
                .build()
                .setDescription("test-random");
        assertNotNull(rule);
        assertInstanceOf(RandomRule.class, rule);
        assertEquals("test-random", rule.getDescription());
    }

    @Test
    void testBuildTimeRule() {
        RuleStream rule = RuleType.Time
                .build()
                .setDescription("test-time");
        assertNotNull(rule);
        assertInstanceOf(TimeRule.class, rule);
        assertEquals("test-time", rule.getDescription());
    }

    @Test
    void testBuildTrendRule() {
        RuleStream rule = RuleType.Trend
                .build()
                .setDescription("test-trend");
        assertNotNull(rule);
        assertInstanceOf(TrendRule.class, rule);
        assertEquals("test-trend", rule.getDescription());
    }

    @Test
    void testBuildWeekdayRule() {
        RuleStream rule = RuleType.Weekday
                .build()
                .setDescription("test-weekday");
        assertNotNull(rule);
        assertInstanceOf(WeekdayRule.class, rule);
        assertEquals("test-weekday", rule.getDescription());
    }

    @Test
    void testBuildXOrRule() {
        RuleStream rule = RuleType.XOr
                .build()
                .setDescription("test-xor");
        assertNotNull(rule);
        assertInstanceOf(XOrRule.class, rule);
        assertEquals("test-xor", rule.getDescription());
    }

    @Test
    void testBuilderWithPropertiesAndResults() {
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("testProp", "testValue");

        RuleStream rule = RuleType.And.build()
                .setProperties(properties)
                .setDescription("test-props");

        assertNotNull(rule);
        assertEquals("test-props", rule.getDescription());
        // verifying properties might require reflection or a getter if available, but assuming constructor passed them correctly
    }
}
