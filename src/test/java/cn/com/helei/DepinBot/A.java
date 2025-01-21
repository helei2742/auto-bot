//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStreamWriter;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Scanner;
//
//class Main {
//
//    public static void main(String[] args) throws IOException {
//        URL url = new URL("https://api.depined.org/api/user/register");
//        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//        httpConn.setRequestMethod("POST");
//
//        httpConn.setRequestProperty("accept", "application/json, text/plain, */*");
//        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
//        httpConn.setRequestProperty("content-type", "application/json");
//        httpConn.setRequestProperty("origin", "https://app.depined.org");
//        httpConn.setRequestProperty("priority", "u=1, i");
//        httpConn.setRequestProperty("referer", "https://app.depined.org/");
//        httpConn.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"");
//        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
//        httpConn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
//        httpConn.setRequestProperty("sec-fetch-dest", "empty");
//        httpConn.setRequestProperty("sec-fetch-mode", "cors");
//        httpConn.setRequestProperty("sec-fetch-site", "same-site");
//        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
//
//        httpConn.setDoOutput(true);
//        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
//        writer.write("{\"email\":\"19141273392@163.com\",\"password\":\"(nklh9tEcz*Vjs\"}");
//        writer.flush();
//        writer.close();
//        httpConn.getOutputStream().close();
//
//        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
//                ? httpConn.getInputStream()
//                : httpConn.getErrorStream();
//        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
//        String response = s.hasNext() ? s.next() : "";
//        System.out.println(response);
//    }
//}
//register
//
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Scanner;
//
//class Main {
//
//    public static void main(String[] args) throws IOException {
//        URL url = new URL("https://api.depined.org/api/user/overview/profile");
//        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//        httpConn.setRequestMethod("GET");
//
//        httpConn.setRequestProperty("accept", "application/json, text/plain, */*");
//        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
//        httpConn.setRequestProperty("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6IjE5MTQxMjczMzkyQDE2My5jb20iLCJpZCI6IjgzYzA0MDYyLWU2MTAtNDRhYS1iNjA2LTg3YzFhYWI2ZmE4ZiJ9.os8R9tP8Zv9ZwOTcRqSedRLMhtKv38OhIu0Or6FMvL8");
//        httpConn.setRequestProperty("origin", "https://app.depined.org");
//        httpConn.setRequestProperty("priority", "u=1, i");
//        httpConn.setRequestProperty("referer", "https://app.depined.org/");
//        httpConn.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"");
//        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
//        httpConn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
//        httpConn.setRequestProperty("sec-fetch-dest", "empty");
//        httpConn.setRequestProperty("sec-fetch-mode", "cors");
//        httpConn.setRequestProperty("sec-fetch-site", "same-site");
//        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
//
//        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
//                ? httpConn.getInputStream()
//                : httpConn.getErrorStream();
//        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
//        String response = s.hasNext() ? s.next() : "";
//        System.out.println(response);
//    }
//}
//
//
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStreamWriter;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Scanner;
//
//class Main {
//
//    public static void main(String[] args) throws IOException {
//        URL url = new URL("https://api.depined.org/api/user/profile-creation");
//        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//        httpConn.setRequestMethod("POST");
//
//        httpConn.setRequestProperty("accept", "application/json, text/plain, */*");
//        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
//        httpConn.setRequestProperty("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6IjE5MTQxMjczMzkyQDE2My5jb20iLCJpZCI6IjgzYzA0MDYyLWU2MTAtNDRhYS1iNjA2LTg3YzFhYWI2ZmE4ZiJ9.os8R9tP8Zv9ZwOTcRqSedRLMhtKv38OhIu0Or6FMvL8");
//        httpConn.setRequestProperty("content-type", "application/json");
//        httpConn.setRequestProperty("origin", "https://app.depined.org");
//        httpConn.setRequestProperty("priority", "u=1, i");
//        httpConn.setRequestProperty("referer", "https://app.depined.org/");
//        httpConn.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"");
//        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
//        httpConn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
//        httpConn.setRequestProperty("sec-fetch-dest", "empty");
//        httpConn.setRequestProperty("sec-fetch-mode", "cors");
//        httpConn.setRequestProperty("sec-fetch-site", "same-site");
//        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
//
//        httpConn.setDoOutput(true);
//        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
//        writer.write("{\"step\":\"username\",\"username\":\"123\"}");
//        writer.flush();
//        writer.close();
//        httpConn.getOutputStream().close();
//
//        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
//                ? httpConn.getInputStream()
//                : httpConn.getErrorStream();
//        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
//        String response = s.hasNext() ? s.next() : "";
//        System.out.println(response);
//    }
//}
//
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStreamWriter;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Scanner;
//
//class Main {
//
//    public static void main(String[] args) throws IOException {
//        URL url = new URL("https://api.depined.org/api/user/profile-creation");
//        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//        httpConn.setRequestMethod("POST");
//
//        httpConn.setRequestProperty("accept", "application/json, text/plain, */*");
//        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
//        httpConn.setRequestProperty("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6IjE5MTQxMjczMzkyQDE2My5jb20iLCJpZCI6IjgzYzA0MDYyLWU2MTAtNDRhYS1iNjA2LTg3YzFhYWI2ZmE4ZiJ9.os8R9tP8Zv9ZwOTcRqSedRLMhtKv38OhIu0Or6FMvL8");
//        httpConn.setRequestProperty("content-type", "application/json");
//        httpConn.setRequestProperty("origin", "https://app.depined.org");
//        httpConn.setRequestProperty("priority", "u=1, i");
//        httpConn.setRequestProperty("referer", "https://app.depined.org/");
//        httpConn.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"");
//        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
//        httpConn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
//        httpConn.setRequestProperty("sec-fetch-dest", "empty");
//        httpConn.setRequestProperty("sec-fetch-mode", "cors");
//        httpConn.setRequestProperty("sec-fetch-site", "same-site");
//        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
//
//        httpConn.setDoOutput(true);
//        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
//        writer.write("{\"step\":\"description\",\"description\":\"Other\"}");
//        writer.flush();
//        writer.close();
//        httpConn.getOutputStream().close();
//
//        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
//                ? httpConn.getInputStream()
//                : httpConn.getErrorStream();
//        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
//        String response = s.hasNext() ? s.next() : "";
//        System.out.println(response);
//    }
//}
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStreamWriter;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Scanner;
//
//class Main {
//
//    public static void main(String[] args) throws IOException {
//        URL url = new URL("https://api.depined.org/api/access-code/referal");
//        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//        httpConn.setRequestMethod("POST");
//
//        httpConn.setRequestProperty("accept", "*/*");
//        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
//        httpConn.setRequestProperty("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6IjE5MTQxMjczMzkyQDE2My5jb20iLCJpZCI6IjgzYzA0MDYyLWU2MTAtNDRhYS1iNjA2LTg3YzFhYWI2ZmE4ZiJ9.os8R9tP8Zv9ZwOTcRqSedRLMhtKv38OhIu0Or6FMvL8");
//        httpConn.setRequestProperty("content-type", "application/json");
//        httpConn.setRequestProperty("origin", "https://app.depined.org");
//        httpConn.setRequestProperty("priority", "u=1, i");
//        httpConn.setRequestProperty("referer", "https://app.depined.org/");
//        httpConn.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"");
//        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
//        httpConn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
//        httpConn.setRequestProperty("sec-fetch-dest", "empty");
//        httpConn.setRequestProperty("sec-fetch-mode", "cors");
//        httpConn.setRequestProperty("sec-fetch-site", "same-site");
//        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
//
//        httpConn.setDoOutput(true);
//        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
//        writer.write("{\"referral_code\":\"123123\"}");
//        writer.flush();
//        writer.close();
//        httpConn.getOutputStream().close();
//
//        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
//                ? httpConn.getInputStream()
//                : httpConn.getErrorStream();
//        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
//        String response = s.hasNext() ? s.next() : "";
//        System.out.println(response);
//    }
//}
