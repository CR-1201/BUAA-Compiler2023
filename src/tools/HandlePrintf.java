package tools;

import java.util.ArrayList;

public class HandlePrintf {
        /**
         * 这个方法会将 formString 按照 %d 截断，返回分开的字符串 list，比如
         * "cr%d%dcr%d" :  ["cr", "%d", "%d", "cr", "%d"]
         * "" : []
         * @param formString  格式化字符串
         * @return ans        截断的字符串List
         */
        public static ArrayList<String> handleString(String formString){
            ArrayList<String> ans = new ArrayList<>();
            int cur = 0;
            formString = formString.replace("\\n", "\\0a");
            if (formString.length() > 2){
                formString = formString.substring(1, formString.length() - 1); // 去除两端引号
                while (cur < formString.length()) {
                    int index = formString.indexOf("%d", cur);
                    if (index == -1){
                        ans.add(formString.substring(cur));
                        break;
                    }else{
                        if (cur != index){
                            ans.add(formString.substring(cur, index));
                        }
                        ans.add(formString.substring(index, index + 2));
                        cur = index + 2;
                    }
                }
            }
            return ans;
        }
        public static int StringLength(String s){
            int len = 0;
            String[] split = s.split("\\\\0a");
            for (String s1 : split){
                len += s1.length();
            }
            int cur = 0;
            while (cur < s.length()){
                int index = s.indexOf("\\0a", cur);
                if (index == -1){
                    break;
                }else{
                    len += 1;
                    cur = index + 1;
                }
            }
            return len;
        }
}
