/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zahnen
 */
public class JsonNestingTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNestingTracker.class);

    private List<String> lastPath = new ArrayList<>();
    private Map<String, Integer> lastMultiplicityLevels = new HashMap<>();
    private int pathDiffersAt;
    private int multiplicityDiffersAt;
    private List<String> currentCloseActions;
    private List<List<String>> currentOpenActions;

    //TODO provide JsonNestingStrategy, use for open and close

    public void track(List<String> path, List<Integer> multiplicities) {
        this.pathDiffersAt = getPathDiffIndex(path, lastPath);
        Map<String, Integer> nextMultiplicityLevels =  getMultiplicityLevels(path, multiplicities, lastMultiplicityLevels);
        this.multiplicityDiffersAt = getMultiplicityDiffIndex(path, nextMultiplicityLevels, lastMultiplicityLevels);

        boolean inArray = false;
        if (multiplicityDiffersAt > -1) {
            String element = path.get(multiplicityDiffersAt);
            String fieldName = element.contains("[") ? element.substring(element.indexOf("[") + 1, element.indexOf("]")) : element;
            inArray = differsAt() == multiplicityDiffersAt && nextMultiplicityLevels.getOrDefault(fieldName, 1) > 1;
        }

        this.currentCloseActions = getCloseActions(lastPath, differsAt(), inArray);
        this.currentOpenActions = getOpenActions(path, differsAt(), inArray);

        this.lastMultiplicityLevels = nextMultiplicityLevels;
        this.lastPath = path;
    }

    public int differsAt() {
        return multiplicityDiffersAt == -1 ? pathDiffersAt : Math.min(pathDiffersAt, multiplicityDiffersAt);
    }

    public List<String> getCurrentCloseActions() {
        return currentCloseActions;
    }

    public List<List<String>> getCurrentOpenActions() {
        return currentOpenActions;
    }

    public int getCurrentMultiplicityLevel(String multiplicityKey) {
        return lastMultiplicityLevels.getOrDefault(multiplicityKey, 1);
    }

    private List<String> getCloseActions(List<String> previousPath, int nextPathDiffersAt, boolean inArray) {
        List<String> actions = new ArrayList<>();

        for (int i = previousPath.size()-1; i >= nextPathDiffersAt; i--) {
            String element = previousPath.get(i);

            boolean closeObject = i < previousPath.size() - 1;
            // omit when value array (end of path array)
            boolean inValueArray = inArray && previousPath.size() -1 == nextPathDiffersAt;
            //omit when already inside of object array
            boolean inObjectArray = closeObject && inArray && i == nextPathDiffersAt;
            boolean closeArray = element.contains("[") && !inValueArray && !inObjectArray;//(closeObject && i == nextPathDiffersAt && i > 0);

            if (closeObject) {
                actions.add("OBJECT");
            }
            if (closeArray) {
                actions.add("ARRAY");
            }
        }

        return actions;
    }

    private List<List<String>> getOpenActions(List<String> nextPath, int nextPathDiffersAt, boolean inArray) {
        List<List<String>> actions = new ArrayList<>();

        for (int i = nextPathDiffersAt; i < nextPath.size(); i++) {
            String element = nextPath.get(i);

            boolean openObject = i < nextPath.size() - 1;
            // omit when value array (end of path array)
            boolean beforeOrInValueArray = i == nextPath.size()-1;
            //omit when already inside of object array
            boolean inObjectArray = openObject && inArray && i == nextPathDiffersAt;
            boolean openArray = element.contains("[") && !beforeOrInValueArray && !inObjectArray;

            List<String> a = new ArrayList<>();
            if (openArray) {
                a.add("ARRAY");
            }
            if (openObject) {
                a.add("OBJECT");
            }
            actions.add(a);
        }

        return actions;
    }

    private int getPathDiffIndex(List<String> path, List<String> path2) {
        // find index where path2 and path start to differ
        int i;
        for (i = 0; i < path2.size() && i < path.size(); i++) {
            if (!Objects.equals(path2.get(i), path.get(i))) break;
        }
        return i;
    }

    private Map<String, Integer> getMultiplicityLevels(List<String> path, List<Integer> multiplicities, Map<String, Integer> previousMultiplicityLevels) {
        final int[] current = {0};
        final Map<String, Integer> nextMultiplicityLevels = new HashMap<>(previousMultiplicityLevels);

        path.stream()
            .filter(element -> element.contains("[") /*&& !(path.indexOf(element) == path.size() - 1)*/)
            .map(element -> element.substring(element.indexOf("[") + 1, element.indexOf("]")))

            .forEach(multiplicityKey -> {

                int currentMultiplicityLevel = multiplicities.size() > current[0] ? multiplicities.get(current[0]) : 1;
                nextMultiplicityLevels.putIfAbsent(multiplicityKey, currentMultiplicityLevel);
                int lastMultiplicityLevel = previousMultiplicityLevels.getOrDefault(multiplicityKey, 1);

                LOGGER.debug("{} {} {}", multiplicityKey, currentMultiplicityLevel, lastMultiplicityLevel);

                if (!Objects.equals(lastMultiplicityLevel, currentMultiplicityLevel)) {
                    nextMultiplicityLevels.put(multiplicityKey, currentMultiplicityLevel);
                }

                current[0]++;
            });

        return nextMultiplicityLevels;
    }



    private int getMultiplicityDiffIndex(List<String> path, Map<String, Integer> nextMultiplicityLevels, Map<String, Integer> previousMultiplicityLevels) {
        int currentIndex = 0;

        for (String element : path) {
            if (element.contains("[")) {
                String multiplicityKey = element.substring(element.indexOf("[") + 1, element.indexOf("]"));
                if (!Objects.equals(nextMultiplicityLevels.get(multiplicityKey), previousMultiplicityLevels.get(multiplicityKey))) {
                    return currentIndex;
                }

                currentIndex++;
            }
        }

        return -1;
    }
}
