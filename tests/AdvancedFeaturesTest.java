package test;

import exceptions.AuthenticationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.Test;
import server.AdvancedChatMediator;
import server.LoggerService;
import server.PasswordPolicyValidator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AdvancedFeaturesTest {

    @Test
    public void testValidPasswordPolicy() {
        try {
            PasswordPolicyValidator.validatePassword("Pass123");
        } catch (AuthenticationException exception) {
            fail("A valid password threw an exception: " + exception.getMessage());
        }
    }

    @Test(expected = AuthenticationException.class)
    public void testPasswordTooShort() throws AuthenticationException {
        PasswordPolicyValidator.validatePassword("P1a");
    }

    @Test(expected = AuthenticationException.class)
    public void testPasswordWithoutDigits() throws AuthenticationException {
        PasswordPolicyValidator.validatePassword("PasswordOnly");
    }

    @Test
    public void testChatAvailabilityAndBusyState() {
        String suffix = UUID.randomUUID().toString();
        String userA = "junit_a_" + suffix;
        String userB = "junit_b_" + suffix;
        String userC = "junit_c_" + suffix;
        AdvancedChatMediator mediator = AdvancedChatMediator.getInstance();

        String roomId = mediator.createOneOnOneChat(userA, userB);
        assertNotNull("A chat between two free users should start", roomId);
        assertTrue("User A should be marked busy", mediator.isUserInChat(userA));
        assertTrue("User B should be marked busy", mediator.isUserInChat(userB));
        assertTrue("A busy user cannot start another chat", mediator.createOneOnOneChat(userA, userC) == null);

        mediator.leaveChat(userA);
        assertFalse("User A should be free after leaving the chat", mediator.isUserInChat(userA));
        assertFalse("The remaining user should be free when the one-on-one chat closes", mediator.isUserInChat(userB));
    }

    @Test
    public void testLoggerFileCreationAndContent() throws IOException {
        String entry = "JUnit test log entry " + UUID.randomUUID().toString();
        LoggerService.getInstance().log(LoggerService.LogType.SYSTEM, entry);

        File logFile = new File("logs" + File.separator + LoggerService.LogType.SYSTEM.getFileName());
        assertTrue("The system log file should exist on disk", logFile.isFile());
        String content = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("The written entry should be present in the log", content.contains(entry));
    }
}
