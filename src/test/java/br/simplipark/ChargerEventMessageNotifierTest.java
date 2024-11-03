package br.simplipark;

import br.simplipark.chatbot.messagedispatcher.MessageDispatcher;
import br.simplipark.evcs.UserChargerService;
import br.simplipark.evcs.model.ChargeEvent;
import br.simplipark.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

class ChargerEventMessageNotifierTest {

    private MessageDispatcher messageDispatcher;

    private UserChargerService userChargerService;

    private static final String NUMBERS_TO_NOTIFY = "1234567890,0987654321";

    @BeforeEach
    void setUp() {
        messageDispatcher = mock(MessageDispatcher.class);
        userChargerService = mock(UserChargerService.class);
    }

    @Test
    void shouldSendMessageWhenEventIsReceived() {
        mockSampleChargeEventTriggering();

        new ChargerEventMessageNotifier(messageDispatcher, userChargerService, NUMBERS_TO_NOTIFY);

        verify(messageDispatcher, times(2)).sendTemplateMessage(anyString(), anyString(), anyList());
    }

    @Test
    void shouldntSendMessagesWhenContactListIsEmpty() {
        mockSampleChargeEventTriggering();

        new ChargerEventMessageNotifier(messageDispatcher, userChargerService, "");

        verify(messageDispatcher, never()).sendTemplateMessage(anyString(), anyString(), anyList());
    }

    private void mockSampleChargeEventTriggering() {
        doAnswer(invocation -> {
            Consumer<ChargeEvent> consumer = invocation.getArgument(0);

            consumer.accept(createSampleChargeEvent());

            return null;
        }).when(userChargerService).addListener(any());
    }

    private static ChargeEvent createSampleChargeEvent() {
        return ChargeEvent.startedCharge(TestUtils.createSampleUser(), TestUtils.createSampleCharger(), TestUtils.createSampleChargingData());
    }
}
