package com.github.eirslett.maven.plugins.frontend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Utils {
    public static List<String> merge(List<String> first, List<String> second) {
        ArrayList<String> result = new ArrayList<String>(first);
        result.addAll(second);
        return result;
    }
    public static List<String> prepend(String first, List<String> list){
        return merge(Arrays.asList(first), list);
    }
}
