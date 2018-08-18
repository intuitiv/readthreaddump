import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadThreadDump {
    public static void main(String[] args) throws IOException {
        String folder = args[0];
        String logFile = args[1];
        File f = new File(folder, logFile);
        String fileName = folder + "/dump_";
        String detailsFile = folder + "/det_";
        int count = 0;
        BufferedReader br = new BufferedReader(new FileReader(f));

        BufferedReader io = new BufferedReader(new InputStreamReader(System.in));

        String line = br.readLine();
        boolean inProgress = false;
        String dump = "";
        String details = "";
        while (line != null) {
            if (line.contains("--- Thread Dump Starts ---")) {
                inProgress = true;
                dump += "\n" + line;
            }

            if (line.contains("--- Thread Dump Complete ---")) {
                inProgress = false;
                dump += "\n" + line;
                File dF = new File(fileName + (count) + ".txt");
                BufferedWriter bw = new BufferedWriter(new FileWriter(dF));
                bw.write(dump);
                bw.close();

                dF = new File(detailsFile + (count) + ".txt");
                bw = new BufferedWriter(new FileWriter(dF));
                bw.write(details);
                details = "";
                dump = "";
                bw.close();
                count++;
            }

            if (inProgress) {
                if (line.startsWith("\"")) {
                    String str = getStack(line, br);
                    if (str != null) {
                        String detail = getDetails(str);
                        if (shouldSaveStack(detail)) {
                            details += "\n" + detail;
                        }

                        dump += "\n" + str;
                    }
                }
            }

            line = br.readLine();
        }
        br.close();

    }

    private static String getStack(String line, BufferedReader br) throws IOException {
        String stack = line;
        boolean consider = false;
        boolean startCheck = false;
        while (true) {
            String next = br.readLine();
            if (!startCheck) {
                if (!next.trim().startsWith("at ")) {
                    stack = readLine(stack, next);
                    continue;
                } else {
                    startCheck = true;
                }
            }
            if (consider || next.trim().startsWith("at com.pega.")) {
                consider = true;

                stack = readLine(stack, next);
                if (next.contains("Locked synchronizers")) {
                    if (!consider) {
                        return null;
                    }
                    while (next.trim().length() != 0) {
                        next = br.readLine();
                        stack = readLine(stack, next);
                    }
                    break;
                }
            } else if (next.contains("Locked synchronizers")) {
                return null;
            }
        }
        return stack;
    }

    private static String getDetails(String stack) {
        String modi = stack.replaceAll("\n", " ").replaceAll("\t", " ");
        String iRet = "";
        String threadNamePatern = "([\\d-\\w\\s()\\[\\]#\\/.:{}]+)";
        Pattern p = Pattern.compile(
                "\"" + threadNamePatern + "\"" + " Id=\\d+ in (\\w+) ([running in native()])*((on lock=([\\w.$@]+)([\\s]+owned by " +
                        threadNamePatern + " Id=[\\d]+)* )*)*BlockedCount : [\\d]+, BlockedTime : [-\\d]+, WaitedCount : [\\d]+, WaitedTime : " +
                        "[-\\d]+ [\\s]+ at ([\\w.]+)");
        Matcher m = p.matcher(modi);
        boolean b = m.find();
        String threadName = m.group(1);
        String state = m.group(2);
        iRet = threadName + "\t" + state;
        if (!state.equals("RUNNABLE")) {
            iRet += "\t" + m.group(8);
            // iRet += "\t" + m.group(4);
        }

        String method = m.group(9);
        String arr[] = method.split("\\.");
        iRet = arr[arr.length - 2] + "." + arr[arr.length - 1] + "\t" + iRet;
        return iRet;
    }

    private static boolean shouldSaveStack(String detail) {
        String methdos[] = { /*"RequestorThreadSync.lockAttempt",*/ "ServiceListenerBaseImpl.sleep",
                "ConditionObjectWrapper.await", "PresenceSessionStateTrackerDaemon.sleepWithGracefulInterrupt",
                "PRTimer.nextTask", "AbstractDaemon.run", "LicenseDaemonImpl.run", "UsageDaemonImpl.run",
                "MQListener.processMessages", "httpclient.MultiThreadedHttpConnectionManager",
                "OperationClient.prepareMessageContext", "PassivationDaemon.run", /*"HttpParser.readRawLine",*/
                "PushSubscriptionCleanerDaemon.sleepWithGracefulInterrupt", "ThreadDumpHelper.dumpThreadInfoWithLocks",
                "AbstractContext.touch", "ServiceClient.sendReceive", "SOAPHeaderImpl.checkParent",
                "AxisDescription.getParameter", "ServiceContext.createOperationContext", "Log4JLogger.isTraceEnabled",
                "ServiceClient.fillSOAPEnvelope", "OperationContext.getMessageContext", "UIDGenerator.generateURNString",
                "interfaces.EngineLocal", "SelectorUtil.select", "LinkedTransferQueue.awaitMatch", "log4j.AsyncAppender", "util.HashedWheelTimer",
                "NioServerBoss.select", "ttl.IndicesTTLService", "threadpool.ThreadPool", "ClockDaemon.nextTask"};
        for (int i = 0; i < methdos.length; i++) {
            if (detail.startsWith(methdos[i])) {
                return false;
            }
        }

        return true;
    }

    static String readLine(String stack, String line) {
        return stack + "\n" + line;
    }
}
