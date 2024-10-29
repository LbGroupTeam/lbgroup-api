package br.simplipark.chatbot.e2e;

import br.simplipark.chatbot.e2e.integrations.MessageAssertionService;
import br.simplipark.chatbot.e2e.integrations.MessageDispatcherTestService;
import br.simplipark.chatbot.e2e.integrations.PaymentTestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@E2eTest
class ChatbotE2eTest {

    @Autowired
    private MessageDispatcherTestService messageSender;

    @Autowired
    private MessageAssertionService messageAssertionService;

    @Autowired
    private PaymentTestDispatcher paymentTestDispatcher;

    @Test
    void shouldStartAndStopAndPaySuccessfully() throws Exception {
        messageSender.sendMessage("Oi");

        messageAssertionService.assertLastReceivedMessageEquals("Seja bem vindo(a)!\n" +
                                                                "Por favor, digite seu CPF para continuarmos.");

        messageSender.sendMessage("....");

        messageAssertionService.assertLastReceivedMessageEquals("CPF inválido. Por favor, digite um CPF válido.");

        messageSender.sendMessage("53451952009");

        messageAssertionService.assertLastReceivedMessageEquals("Usuário não encontrado. Por favor, digite um CPF cadastrado.");

        messageSender.sendMessage("62115264070");

        messageAssertionService.assertLastReceivedMessageContains("O que você gostaria de fazer?");
        messageAssertionService.assertLastReceivedMessageContains("1 - Iniciar carga");

        messageSender.sendMessage("1");

        messageAssertionService.assertLastReceivedMessageContains("1 - ECO Vaga - DIREITA");

        messageSender.sendMessage("1");

        messageAssertionService.assertLastReceivedMessageContains("Você está conectado ao carregador ECO Vaga - DIREITA");

        messageSender.sendMessage("parar");

        messageAssertionService.assertLastReceivedMessageContains("Carga finalizada manualmente.");
        messageAssertionService.assertLastReceivedMessageContains("Qual será a modalidade de pagamento?");

        messageSender.sendMessage("1");

        messageAssertionService.assertLastReceivedMessageContains("Você escolheu pagar com LBCoins.");
        messageAssertionService.assertLastReceivedMessageContains("Confirma o pagamento?");

        messageSender.sendMessage("confirmar");

        messageAssertionService.assertLastReceivedMessageContains("Pagamento confirmado! Obrigado por utilizar nossa plataforma! Até a próxima.");
    }

    @Test
    void shouldBeAbleToBuyLbCoinsSucessfully() throws Exception {
        messageSender.sendMessage("Oi");

        messageSender.sendMessage("62115264070");

        messageSender.sendMessage("2");

        paymentTestDispatcher.simulateSucessfullLbCoinsPurchase();

        messageAssertionService.assertLastReceivedMessageContains("Compra efetuada com sucesso.");
    }

    @Test
    void shouldBeAbleToPayWithCreditCardSucessfully() throws Exception {
        messageSender.sendMessage("Oi");

        messageSender.sendMessage("62115264070");

        messageSender.sendMessage("1");

        messageSender.sendMessage("1");

        messageSender.sendMessage("parar");

        messageSender.sendMessage("2");

        paymentTestDispatcher.simulateSucessfullCreditCardPayment();

        messageAssertionService.assertLastReceivedMessageContains("Pagamento confirmado! Obrigado por utilizar nossa plataforma! Até a próxima.");
    }
}