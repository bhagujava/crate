/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.integrationtests;

import io.crate.testing.TestingHelpers;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;

public class AnyIntegrationTest extends SQLTransportIntegrationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testAnyOnArrayLiteralDeleteUpdateSelect() throws Exception {
        execute("create table t (" +
                "  i int," +
                "  ia array(int)," +
                "  s string," +
                "  sa array(string)" +
                ") clustered into 1 shards with (number_of_replicas=0)");
        ensureYellow();
        execute("insert into t (i, ia, s, sa) values (?, ?, ?, ?)", new Object[][]{
                {1, new Object[]{1,2,3}, "foo", new Object[]{"abc", "def", "ghi"}},
                {2, new Object[]{3,4,5}, "bar", new Object[]{"rst", "uvw", "aaa"}},
                {3, new Object[]{7,8,9}, "baz", new Object[]{"bar", "baz"}}
        });
        execute("refresh table t");

        execute("select i, s from t where i = ANY([1,2,4]) order by i");
        assertThat(response.rowCount(), is(2L));
        assertThat((Integer)response.rows()[0][0], is(1));
        assertThat((Integer)response.rows()[1][0], is(2));

        execute("select i, sa from t where 'ba%' not like ANY(sa) order by i");
        assertThat(response.rowCount(), is(2L));
        assertThat((Integer)response.rows()[0][0], is(1));
        assertThat((Integer)response.rows()[1][0], is(2));

        execute("update t set s='updated' where i > ANY([?, ?, ?])", new Object[]{2, 4, 5});
        assertThat(response.rowCount(), is(1L));
        execute("refresh table t");

        execute("select s from t order by i");
        assertThat(TestingHelpers.getColumn(response.rows(), 0), Matchers.<Object>arrayContaining("foo", "bar", "updated"));

        execute("delete from t where 'a%' like ANY (sa)");
        assertThat(response.rowCount(), is(2L));
        execute("refresh table t");

        execute("select * from t");
        assertThat(response.rowCount(), is(1L));
        assertThat(TestingHelpers.printedTable(response.rows()), is("3| [7, 8, 9]| updated| [bar, baz]\n"));
    }

    @Test
    public void testAnyOnArrayLiteral() throws Exception {
        execute("create table t (b byte, sa array(string), s string) clustered into 1 shards with (number_of_replicas=0)");
        ensureYellow();
        execute("insert into t (b, sa, s) values (1, ['foo', 'bar'], 'goo')," +
                "(2, ['bar', 'baz'], 'zar')," +
                "(3, ['funky', 'shizzle'], 'ziffle')");
        execute("refresh table t");

        execute("select * from t where 'bar' = ANY(sa)");
        assertThat(response.rowCount(), Is.is(2L));

        execute("select b from t where b = ANY([1, 2, 4]) order by b");
        assertThat(response.rowCount(), Is.is(2L));
        assertThat((Byte)response.rows()[0][0], Is.is((byte) 1));
        assertThat((Byte)response.rows()[1][0], Is.is((byte) 2));

        execute("select * from t where b != ANY([1, 2, 4]) order by b");
        assertThat(response.rowCount(), Is.is(3L)); // all rows does not contain at least one of the array elements

        execute("select b from t where b <= ANY([-1, 0, 1])");
        assertThat(response.rowCount(), Is.is(1L));
        assertThat((Byte)response.rows()[0][0], Is.is((byte) 1));

        execute("select b from t where s like ANY(['%ar', 'go%']) order by b DESC");
        assertThat(response.rowCount(), Is.is(2L));
        assertThat((Byte)response.rows()[0][0], Is.is((byte) 2));
        assertThat((Byte)response.rows()[1][0], Is.is((byte) 1));
    }

    @Test
    public void testAnyOnArrayLiteralWithNullElements() throws Exception {
        execute("create table t (s string)");
        ensureYellow();
        execute("insert into t (s) values ('foo'), (null)");
        execute("refresh table t");
        execute("select * from t where s = ANY (['foo', 'bar', null])");
        assertThat(response.rowCount(), is(1L));

        execute("select * from t where s = ANY ([null])");
        assertThat(response.rowCount(), is(0L));
    }
}
