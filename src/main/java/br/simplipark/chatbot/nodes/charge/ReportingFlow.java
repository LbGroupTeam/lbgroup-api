package br.simplipark.chatbot.nodes.charge;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.chatbot.ChatbotUser;
import br.simplipark.chatbot.ConversationPathManager;
import br.simplipark.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.simplipark.chatbot.nodes.MainConversationStage;
import br.simplipark.user.reporting.ReportData;
import br.simplipark.user.reporting.ReportingService;
import br.simplipark.util.files.PdfReportGenerator;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReportingFlow {

    private final QueueMessageDispatcher queueMessageDispatcher;

    private final ConversationPathManager conversationPathManager;

    private final ReportingService reportingService;
    private final PdfReportGenerator pdfReportGenerator;

    public ReportingFlow(QueueMessageDispatcher queueMessageDispatcher, ConversationPathManager conversationPathManager, ReportingService reportingService, PdfReportGenerator pdfReportGenerator) {
        this.queueMessageDispatcher = queueMessageDispatcher;
        this.conversationPathManager = conversationPathManager;
        this.reportingService = reportingService;
        this.pdfReportGenerator = pdfReportGenerator;
    }

    public void handleMessage(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
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
}
