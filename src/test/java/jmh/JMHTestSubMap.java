/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package jmh;


import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Benchmark)
@SuppressWarnings("NewClassNamingConvention")
public class JMHTestSubMap {
    private static TestMap map;

    @Test
    void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.Throughput)
            .warmupTime(TimeValue.seconds(1))
            .warmupIterations(5)
            .threads(1)
            .measurementIterations(5)
            .forks(1)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();
        new Runner(opt).run();
    }

    @Setup(Level.Invocation)
    public void setup() {
        map = new TestMap();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void removeEachBestCase() {
        for (int i = 80; i < map.size(); i++) {
            map.remove(i);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void removeEachWorstCase() {
        for (int i = 20; i < map.size(); i++) {
            map.remove(i);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void subMapBestCase(Blackhole blackhole) {
        NavigableMap<Integer, Integer> nMap = new TreeMap<>(map);
        Map<Integer, Integer> newMap = new HashMap<>(nMap.subMap(0, 20));
        blackhole.consume(nMap);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void subMapWorstCase(Blackhole blackhole) {
        NavigableMap<Integer, Integer> nMap = new TreeMap<>(map);
        Map<Integer, Integer> newMap = new HashMap<>(nMap.subMap(0, 80));
        blackhole.consume(nMap);
    }

    public static class TestMap extends HashMap<Integer, Integer> {
        {
            for (int i = 0; i < 100; i++) {
                int size = size();
                put(size, size);
            }
        }
    }
}
