package net.bytebutcher.burpsendtoextension.models;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bytebutcher.burpsendtoextension.models.placeholder.behaviour.PlaceholderBehaviour;
import net.bytebutcher.burpsendtoextension.parser.CommandObjectFileParser;
import net.bytebutcher.burpsendtoextension.utils.OsUtils;

import java.util.ArrayList;
import java.util.List;

public class Config {

    private final IBurpExtenderCallbacks callbacks;
    private BurpExtender burpExtender;
    private String version = "1.3";

    public Config(BurpExtender burpExtender) {
        this.burpExtender = burpExtender;
        this.callbacks = BurpExtender.getCallbacks();
        refreshVersion();
    }

    public void saveSendToTableData(List<CommandObject> sendToTableData) {
        this.callbacks.saveExtensionSetting("SendToTableData",
                CommandObjectFileParser.getParser().toJson(sendToTableData));
    }

    public List<CommandObject> getSendToTableData() {
        List<CommandObject> commandObjectList = new ArrayList<>();
        try {
            String sendToTableData = this.callbacks.loadExtensionSetting("SendToTableData");
            if (sendToTableData == null || sendToTableData.isEmpty() || "[]".equals(sendToTableData)) {
                if (isFirstStart()) {
                    BurpExtender.printOut("Initializing default table data...");
                    commandObjectList = initializeDefaultSendToTableData();
                }
                return commandObjectList;
            }
            return CommandObjectFileParser.getParser()
                    .fromJson(sendToTableData, new TypeToken<List<CommandObject>>() {}.getType());
        } catch (Exception e) {
            BurpExtender.printErr("Error retrieving table data!");
            BurpExtender.printErr(e.toString());
            return commandObjectList;
        }
    }

    private List<CommandObject> initializeDefaultSendToTableData() {
        List<CommandObject> commandObjectList = getDefaultSendToTableData();
        saveSendToTableData(commandObjectList);
        unsetFirstStart();
        return commandObjectList;
    }

    public List<CommandObject> getDefaultSendToTableData() {
        String groupFuzz = "fuzz";
        String groupCMS = "cms";
        String groupSQL = "sql";
        String groupSSL = "ssl";
        String groupOther = "other";
        List<PlaceholderBehaviour> defaultPlaceholderBehaviour = Lists.newArrayList();
        return Lists.newArrayList(
                // cms
                new CommandObject("droopescan", "droopescan scan drupal -u %U -t 10", groupCMS, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("mooscan", "mooscan -v --url %U", groupCMS, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("wpscan", "wpscan --url %U --threads 10", groupCMS, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                // fuzz
                new CommandObject("bfac", "bfac --url %U", groupFuzz, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("gobuster", "gobuster -u %U -s 403,404 -w /usr/share/wfuzz/wordlist/general/common.txt", groupFuzz, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("nikto", "nikto %U", groupFuzz, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("wfuzz", "wfuzz -c -w /usr/share/wfuzz/wordlist/general/common.txt --hc 404,403 %U", groupFuzz, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                // sql
                new CommandObject("sqlmap (GET)", "sqlmap -o -u %U --level=5 --risk=3", groupSQL, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("sqlmap (POST)", "sqlmap -r %R  --level=5 --risk=3", groupSQL, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                // ssl
                new CommandObject("sslscan", "sslscan %H:%P", groupSSL, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("sslyze", "sslyze --regular %H:%P", groupSSL, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("testssl", "testssl.sh %H:%P", groupSSL, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                // other
                new CommandObject("Host (%H)", "echo %H", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("Port (%P)", "echo %P", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("Protocol (%T)", "echo %T", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("URL (%U)", "echo %U", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("URL-Path (%A)", "echo %A", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("URL-Query (%Q)", "echo %Q", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("Cookies (%C)", "echo %C", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Method (%M)", "echo %M", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("Selected text (%S)", "echo %S", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("Selected text as file (%F)", "echo %F && cat %F", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Request/-Response as file (%R)", "echo %R && cat %R", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Headers as file (%E)", "echo %E && cat %E", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Body as file (%B)", "echo %B && cat %B", groupOther, ERuntimeBehaviour.RUN_IN_TERMINAL, true, defaultPlaceholderBehaviour)
        );
    }

    private void refreshVersion() {
        if (!this.version.equals(this.callbacks.loadExtensionSetting("version"))) {
            this.callbacks.saveExtensionSetting("version", version);
            setFirstStart();
        }
    }

    private boolean isFirstStart() {
        String isFirstStart = this.callbacks.loadExtensionSetting("isFirstStart");
        return isFirstStart == null || "true".equals(isFirstStart);
    }

    private void setFirstStart() {
        this.callbacks.saveExtensionSetting("isFirstStart", null);
    }

    private void unsetFirstStart() {
        this.callbacks.saveExtensionSetting("isFirstStart", "false");
    }

    public void setRunInTerminalBehaviour(ERunInTerminalBehaviour runInTerminalBehaviour) {
        this.callbacks.saveExtensionSetting("runInTerminalBehaviour", runInTerminalBehaviour.name());
    }

    public ERunInTerminalBehaviour getRunInTerminalBehaviour() {
        String runInTerminalBehaviour = this.callbacks.loadExtensionSetting("runInTerminalBehaviour");
        if (runInTerminalBehaviour == null || runInTerminalBehaviour.isEmpty()) {
            return ERunInTerminalBehaviour.RUN_IN_SINGLE_TERMINAL;
        }
        return ERunInTerminalBehaviour.valueOf(runInTerminalBehaviour);
    }

    public boolean shouldShowRunInTerminalBehaviourChoiceDialog() {
        String showDialog = this.callbacks.loadExtensionSetting("showRunInTerminalBehaviourDialog");
        if (showDialog == null || showDialog.isEmpty()) {
            return true; // Default
        }
        return Boolean.parseBoolean(showDialog);
    }

    public void shouldShowRunInTerminalBehaviourChoiceDialog(boolean status) {
        this.callbacks.saveExtensionSetting("showRunInTerminalBehaviourDialog", String.valueOf(status));
    }

    public String getRunInTerminalCommand() {
        if (OsUtils.isWindows()) {
            return getRunInTerminalCommand("runInTerminalSettingWindows", "cmd  /c start cmd /K %C");
        } else {
            return getRunInTerminalCommand("runInTerminalSettingUnix", "xterm -hold -e %C");
        }
    }

    private String getRunInTerminalCommand(String label, String defaultValue) {
        String runInTerminalSetting = this.callbacks.loadExtensionSetting(label);
        if (runInTerminalSetting == null || runInTerminalSetting.isEmpty()) {
            runInTerminalSetting = defaultValue;
            this.callbacks.saveExtensionSetting(label, runInTerminalSetting);
        }
        return runInTerminalSetting;
    }

    public void setRunInTerminalCommand(String command) {
        if (OsUtils.isWindows()) {
            this.callbacks.saveExtensionSetting("runInTerminalSettingWindows", command);
        } else {
            this.callbacks.saveExtensionSetting("runInTerminalSettingUnix", command);
        }
    }

    public void resetRunInTerminalCommand() {
        this.callbacks.saveExtensionSetting("runInTerminalSettingWindows", "cmd  /c start cmd /K %C");
        this.callbacks.saveExtensionSetting("runInTerminalSettingUnix", "xterm -hold -e %C");

    }

    public void setSafeMode(boolean status) {
        this.callbacks.saveExtensionSetting("safeMode", String.valueOf(status));
    }

    public boolean isSafeModeActivated() {
        String safeMode = this.callbacks.loadExtensionSetting("safeMode");
        if (safeMode == null || safeMode.isEmpty())
            return true;
        return Boolean.parseBoolean(safeMode);
    }
}
