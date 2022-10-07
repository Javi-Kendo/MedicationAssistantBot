package org.universityit.project.medicationsassistantbot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.universityit.project.medicationsassistantbot.config.BotConfig;
import org.universityit.project.medicationsassistantbot.model.Medication;
import org.universityit.project.medicationsassistantbot.model.User;
import org.universityit.project.medicationsassistantbot.model.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;

    static final String HELP_TEXT = "This bot is created.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "Error occurred: ";

    List<Medication> medicationList = listPopulate(new ArrayList<>());

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "get a welcome message"));
        listofCommands.add(new BotCommand("/mydata", "get your data stored"));
        listofCommands.add(new BotCommand("/deletedata", "delete my data"));
        listofCommands.add(new BotCommand("/help", "info how to use this bot"));
        listofCommands.add(new BotCommand("/settings", "set your preferences"));
        listofCommands.add(new BotCommand("/a", "0"));
        listofCommands.add(new BotCommand("/b", "1"));
        listofCommands.add(new BotCommand("/c", "2"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && config.getOwnerId() == chatId) {
                var textToSend = EmojiParser
                        .parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user: users){
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    case "/addMedications":
                        addMedications(chatId);
                        break;
                    case "/getMyMedications":
                        getMedications(chatId);
                        break;
                    case "/checkOrChangeMyData":
                        checkOrChangeData(chatId);
                        break;
                    case "/a":
                        commandAddMedications(chatId, "a", update.getMessage());
                        break;
                    case "/b":
                        commandAddMedications(chatId, "b", update.getMessage());
                        break;
                    case "/c":
                        commandAddMedications(chatId, "c", update.getMessage());
                        break;
                    case "/deleteMyData":
                        deleteUserData(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not recognized " +
                                "(Извините, команда не распознана)");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON)) {
                String text = "Register completed successfully.";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.equals(NO_BUTTON)) {
                String text = "Register cancelled.";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.equals("ADD_BUTTON")) {
                String text = "Type the name of the medication.";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void commandAddMedications(long chatId2, String medicationName, Message msg) {
        SendMessage message = new SendMessage();
        long medicationNumber = -1;
        String status = "";

        for (int i = 0; i < 4; i++) {
            if (medicationName.equals(medicationList.get(i).getName())) {
                medicationNumber = i;
            }
        }
        message.setChatId(String.valueOf(chatId2));

        if (medicationNumber > -1) {
            if(!userRepository.findById(msg.getChatId()).isEmpty()) {

                var chatId = msg.getChatId();
                var chat = msg.getChat();

                User user = userRepository.findById(chatId).get();
                if (medicationName.equals("c") && !user.getUserMedications().contains("a" + ";")) {
                    user.setChatId(chatId);
                    user.setFirstName(chat.getFirstName());
                    user.setLastName(chat.getLastName());
                    user.setUserName(chat.getUserName());
                    user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
                    if (user.getUserMedications().contains(medicationName + ";")) {
                        status = "The medication is already on your list.";
                    } else {
                        user.setUserMedications(user.getUserMedications() + medicationName + ";");
                    }
                    userRepository.save(user);
                    log.info("user saved: " + user);
                } else if (medicationName.equals("c")) {
                    user.setChatId(chatId);
                    user.setFirstName(chat.getFirstName());
                    user.setLastName(chat.getLastName());
                    user.setUserName(chat.getUserName());
                    user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
                    /*if (user.getUserMedications().contains(medicationName + ";")) {
                        status = "The medication is already on your list.";
                    } else {
                        user.setUserMedications(user.getUserMedications() + medicationName + ";");
                    }*/
                    status = "Medications are incompatible.";
                    userRepository.save(user);
                    log.info("user saved: " + user);
                } else {
                    user.setChatId(chatId);
                    user.setFirstName(chat.getFirstName());
                    user.setLastName(chat.getLastName());
                    user.setUserName(chat.getUserName());
                    user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
                    if (user.getUserMedications().contains(medicationName + ";")) {
                        status = "The medication is already on your list.";
                    } else {
                        user.setUserMedications(user.getUserMedications() + medicationName + ";");
                    }
                    userRepository.save(user);
                    log.info("user saved: " + user);
                }
            }
            if (status.length() == 0) {
                message.setText("Successfully!");
            } else {
                message.setText(status);
            }
        }
        executeMessage(message);
    }

    private void deleteUserData(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to delete your data?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void checkOrChangeData(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Select an action.");

        executeMessage(message);
    }

    private void getMedications(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want a list of your medications?");

        executeMessage(message);
    }

    private void addMedications(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to add medications?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Add");
        yesButton.setCallbackData("ADD_BUTTON");

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    /*private void addMedicationInTable(Message msg) {
        if(!userRepository.findById(msg.getChatId()).isEmpty()) {

            var chat = msg.getChat();

            User user = new User();

            user.setUserMedications(chat + ";");

            userRepository.save(user);
            log.info("user add medication: " + user);
        }
    }*/

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()) {

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!\n" +
                "(Привет, " + name + ", приятно познакомиться!)" + " :relaxed:");
        log.info("Replied to user " + name);

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("/addMedications");
        row.add("/getMyMedications");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("/register");
        row.add("/checkOrChangeMyData");
        row.add("/deleteMyData");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private List<Medication> listPopulate(List<Medication> medicationList) {
        Medication medication_A = new Medication();
        Medication medication_B = new Medication();
        Medication medication_C = new Medication();
        Medication medication_D = new Medication();
        Medication medication_E = new Medication();

        medication_A.setName("a");
        medication_A.setMedicationId(0L);
        medication_B.setName("b");
        medication_B.setMedicationId(1L);
        medication_C.setName("c");
        medication_C.setMedicationId(2L);
        medication_D.setName("d");
        medication_D.setMedicationId(3L);
        medication_E.setName("e");
        medication_E.setMedicationId(4L);

        medicationList.add(medication_A);
        medicationList.add(medication_B);
        medicationList.add(medication_C);
        medicationList.add(medication_D);
        medicationList.add(medication_E);

        return medicationList;
    }
}