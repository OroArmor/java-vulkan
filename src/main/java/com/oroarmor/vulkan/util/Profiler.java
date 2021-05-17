/*
 * MIT License
 *
 * Copyright (c) 2021 OroArmor (Eli Orona)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oroarmor.vulkan.util;

import java.util.*;

public class Profiler {
    private final Stack<Long> times;
    private final Stack<ProfilerStep> steps;

    public Profiler(String name) {
        steps = new Stack<>();
        times = new Stack<>();
        steps.push(new ProfilerStep(name));
    }

    public void push(String name) {
        steps.push(steps.peek().getOrCreate(name));
        times.push(System.nanoTime());
    }

    public void pop() {
        if (steps.size() == 1) {
            throw new RuntimeException("Cannot pop root of profiler!");
        }
        long time = System.nanoTime() - times.pop();
        ProfilerStep step = steps.pop();
        step.time.addTime(time);
    }

    public void profile(Runnable runnable, String name) {
        this.push(name);
        runnable.run();
        this.pop();
    }

    public void clear() {
        while (steps.size() > 1) {
            pop();
        }
        steps.peek().clear();
        times.clear();
    }

    public String dump() {
        return steps.get(0).dump(0);
    }

    private static class ProfilerStep {
        private final String name;
        private final Map<String, ProfilerStep> nameToProfile;
        private final List<ProfilerStep> profiles;
        private ProfilerStepTime time;

        public ProfilerStep(String name) {
            this.name = name;
            this.profiles = new ArrayList<>();
            this.nameToProfile = new HashMap<>();
            this.time = new ProfilerStepTime();
        }

        public boolean contains(String name) {
            return nameToProfile.containsKey(name);
        }

        public ProfilerStep getOrCreate(String name) {
            if (contains(name)) {
                return nameToProfile.get(name);
            }
            ProfilerStep profilerStep = new ProfilerStep(name);
            nameToProfile.put(name, profilerStep);
            profiles.add(profilerStep);
            return profilerStep;
        }

        public void clear() {
            time = new ProfilerStepTime();
            for (ProfilerStep step : profiles) {
                step.clear();
            }
            profiles.clear();
        }

        public String dump(int i, long superTotalTime) {
            long averageTime = time.getAverageTime();
            StringBuilder self = new StringBuilder();
            self.append("|   ".repeat(Math.max(0, i)));
            self.append("|=> ");
            self.append(String.format("%s", name));

            if (averageTime != -1) {
                self.append(String.format(" : %d ns", averageTime));
            }

            if (superTotalTime != -1) {
                self.append(String.format(" - %.2f%%", 100d * ((double) averageTime) / superTotalTime));
            }

            profiles.forEach(profilerStep -> {
                String dump = profilerStep.dump(i + 1, averageTime);
                self.append("\n");
                self.append(dump);
            });

            return self.toString();
        }

        public String dump(int i) {
            return dump(i, -1);
        }
    }

    private static class ProfilerStepTime {
        private final List<Long> times = new ArrayList<>();
        private long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE;

        public long getMaxTime() {
            return maxTime;
        }

        public long getMinTime() {
            return minTime;
        }

        public long getAverageTime() {
            if (times.isEmpty()) {
                return -1;
            }
            return times.stream().reduce(0L, Long::sum) / times.size();
        }

        public void addTime(long time) {
            times.add(time);
            if (time < minTime) {
                minTime = time;
            } else if (time > maxTime) {
                maxTime = time;
            }
        }
    }
}
