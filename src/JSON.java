import java.util.HashMap;

class JSON {
    private HashMap<String, String> json;

    JSON(){
        json = new HashMap<>();
    }

    void add(String key, String value){
        json.put(key, value);
    }

    String get(String key){
        return json.get(key);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (String s : json.values()){
            sb.append(s).append(' ');
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
