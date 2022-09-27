package com.sakeri.connetc_wifi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingCheck {
    public int executeProcess(final long timeout, final String command)
            throws IOException, InterruptedException, TimeoutException {
        Process process = Runtime.getRuntime().exec("/system/bin/ping -c 4 " + command, null);
        Worker worker = new Worker(process);
        worker.start();
        try {
            worker.join(timeout);
            if (worker.exit != null){
                return worker.exit;
            } else{
                throw new TimeoutException();
            }
        } catch (InterruptedException ex) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw ex;
        }
        finally {
            process.destroy();
        }
    }

    private static class Worker extends Thread {
        private final Process process;
        private Integer exit;

        private Worker(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            InputStream errorStream = null;
            InputStream inputStream = null;
            try {
                errorStream = process.getErrorStream();
                inputStream = process.getInputStream();
                readStreamInfo(errorStream,inputStream);
                exit = process.waitFor();
                if (exit == 0) {
//                    MyLog.d("wifi_ping_check","子进程正常完成");
                } else {
//                    MyLog.d("wifi_ping_check","子进程异常结束");
                }
                process.destroy();
            } catch (InterruptedException ignore) {
                return;
            }
        }
    }

    /**
     * 读取RunTime.exec运行子进程的输入流 和 异常流
     * @param inputStreams 输入流
     */
    public static void readStreamInfo(InputStream... inputStreams){
        ExecutorService executorService = Executors.newFixedThreadPool(inputStreams.length);
        for (InputStream in : inputStreams) {
            executorService.execute(new MyThread(in));
        }
        executorService.shutdown();
    }
}
class MyThread implements Runnable {

    private InputStream in;
    public MyThread(InputStream in){
        this.in = in;
    }

    @Override
    public void run() {
//        int node = 0;
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "GBK"));
            String line = null;
            for (int j = 0;j<=4;j++){
                line = br.readLine();
                if (line != null){
                    MyLog.d("wifi_ping_inputStream: ",line);
                }
            }
//            while(true){
//                line = br.readLine();
//                if (line == null){
//                    node = node +1;
//                }else {
//                    MyLog.d("wifi_ping_inputStream: ",line);
//                }
//                MyLog.d("wifi_ping_inputStream_node: ",node);
//                if(node == 2){
//                    break;
//                }
//            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                e.printStackTrace();
            }
        }
    }
//    private static int getCheckResult(StringBuffer line) {
//        Pattern pattern = Pattern.compile("(\\d+ms)(\\s+)(TTL=\\d+)", Pattern.CASE_INSENSITIVE);
//        Matcher matcher = pattern.matcher(line);
//        while (matcher.find()) {
//            return 1;
//        }
//        return 0;
//    }
//    private int resultCheck(){
//        int node = getCheckResult(strBuffer);
//        return node;
//    }
}
