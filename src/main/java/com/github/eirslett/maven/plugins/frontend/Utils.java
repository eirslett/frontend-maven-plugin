package com.github.eirslett.maven.plugins.frontend;

import java.io.File;
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

    public static String normalize(String path){
        return path.replace("/", File.separator);
    }

    public static String implode(String separator, List<String> elements){
        String s = "";
        for(int i = 0; i < elements.size(); i++){
            if(i > 0){
                s += " ";
            }
            s += elements.get(i);
        }
        return s;
    }
}
