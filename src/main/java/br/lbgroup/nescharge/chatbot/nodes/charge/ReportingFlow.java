package br.lbgroup.nescharge.chatbot.nodes.charge;

import br.lbgroup.nescharge.chatbot.ChatbotUser;
import br.lbgroup.nescharge.chatbot.ConversationPathManager;
import br.lbgroup.nescharge.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.lbgroup.nescharge.chatbot.nodes.MainConversationStage;
import br.lbgroup.commons.user.UserService;
import br.lbgroup.commons.user.reporting.ReportData;
import br.lbgroup.commons.user.reporting.ReportingService;
import br.lbgroup.commons.util.FormatingUtils;
import br.lbgroup.commons.util.files.PdfReportGenerator;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReportingFlow {

    private final QueueMessageDispatcher queueMessageDispatcher;

    private final ConversationPathManager conversationPathManager;

    private final UserService userService;

    private final ReportingService reportingService;
    private final PdfReportGenerator pdfReportGenerator;

    public ReportingFlow(QueueMessageDispatcher queueMessageDispatcher, ConversationPathManager conversationPathManager, UserService userService, ReportingService reportingService, PdfReportGenerator pdfReportGenerator) {
        this.queueMessageDispatcher = queueMessageDispatcher;
        this.conversationPathManager = conversationPathManager;
        this.userService = userService;
        this.reportingService = reportingService;
        this.pdfReportGenerator = pdfReportGenerator;
    }

    public void handleReportingRequest(ChatbotUser chatbotUser) {
        Optional<ReportData> reportData = reportingService.generateReportForUser(chatbotUser.user());
        if (reportData.isEmpty()) {
            queueMessageDispatcher.queueMessage(chatbotUser, "Não foram encontradas cargas nesse mês.");

            conversationPathManager.navigateTo(chatbotUser, MainConversationStage.GREETING.name());

            return;
        }

        queueMessageDispatcher.queueMessage(chatbotUser, "Segue um arquivo PDF com o seu histórico de cargas desse mês.");

        queueMessageDispatcher.sendFileMessage(chatbotUser, pdfReportGenerator.generatePdfReport(reportData.get()));

        conversationPathManager.navigateTo(chatbotUser, MainConversationStage.GREETING.name());
    }

    public void handleBalanceInquiry(ChatbotUser chatbotUser) {
        String balance = FormatingUtils.roundToTwoDecimals(userService.getLbCoinsBalance(chatbotUser.user()));

        queueMessageDispatcher.queueMessage(chatbotUser, "Seu saldo de LB Coins é de " + balance + " LB Coins.");

        conversationPathManager.navigateTo(chatbotUser, MainConversationStage.GREETING.name());
    }
}
