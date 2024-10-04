package br.simplipark.sandbox;

import br.simplipark.chatbot.messagedispatcher.infobip.InfobipMessageDispatcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled
class InfobipMessageDispatcherSandboxTest {

    @Autowired
    private InfobipMessageDispatcher infobipMessageDispatcher;

    @Test
    void sendMessage() {
        infobipMessageDispatcher.sendMessage("5511934554764", "Teste");
    }

    @Test
    void sandbox() {
//        infobipMessageDispatcher.makeWebHookSettingRequest("https://37ca-191-243-208-26.ngrok-free.app/infobip/receive-message");

//        infobipMessageDispatcher.getWebHookSetting();
    }


}