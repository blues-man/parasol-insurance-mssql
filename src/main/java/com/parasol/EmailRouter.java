package com.parasol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parasol.model.Email;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmailRouter {

    private static final Logger LOG = Logger.getLogger(EmailRouter.class.getName());

    private static final List<String> EXISTING_CUSTOMERS = List.of(
            "Sarah Mitchell", "Robert Chen", "Emily Watson", "James Rodriguez",
            "Patricia O'Brien", "Michael Turner", "Karen Foster", "David Park");

    private static final List<String> CLAIM_KEYWORDS = List.of(
            "accident", "damage", "claim", "injury", "loss", "flood", "fire",
            "collision", "theft", "broken", "wreck", "incident", "stolen");

    private static final List<String> REVIEW_TRIGGERS = List.of(
            "not sure what to do", "what are my options", "what do i do",
            "help?", "collapsed", "sagging", "torn through", "rotted",
            "spider web", "shook", "not great", "sorted out",
            "what now?", "no idea", "not sure how", "what should i",
            "how do i handle");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicLong idSequence = new AtomicLong(0);
    private final ObjectMapper mapper = new ObjectMapper();
    private final EmailStore store;

    public EmailRouter(EmailStore store) {
        this.store = store;
    }

    public void consume(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            String from = node.get("from").asText();
            String subject = node.get("subject").asText();
            String body = node.get("body").asText();

            String senderName = extractSenderName(from);
            String matchedCustomer = findExistingCustomer(senderName);
            String combinedText = body + " " + subject;
            boolean hasClaim = containsClaimKeyword(combinedText);

            String department;
            String reason;

            if (matchedCustomer != null && hasClaim) {
                department = "CLAIMS";
                reason = "Existing customer " + matchedCustomer + " \u2014 possible claim";
            } else if (matchedCustomer != null) {
                department = "RETENTION";
                reason = "Detected existing customer: " + matchedCustomer;
            } else if (hasClaim) {
                department = "CLAIMS";
                reason = "Possible claim from new customer";
            } else if (containsReviewTrigger(combinedText)) {
                department = "REVIEW REQUIRED";
                reason = "Unable to auto-classify \u2014 manual review needed";
            } else {
                department = "ACQUISITION";
                reason = "Unknown customer \u2014 potential new business";
            }

            Email email = new Email(
                    idSequence.incrementAndGet(),
                    from, subject, body,
                    LocalDateTime.now().format(FORMATTER),
                    department, reason);

            store.add(email);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process email", e);
        }
    }

    private String extractSenderName(String from) {
        int lt = from.indexOf('<');
        if (lt > 0) {
            return from.substring(0, lt).trim();
        }
        return from.trim();
    }

    private String findExistingCustomer(String senderName) {
        String lower = senderName.toLowerCase();
        for (String customer : EXISTING_CUSTOMERS) {
            if (lower.contains(customer.toLowerCase())) {
                return customer;
            }
        }
        return null;
    }

    private boolean containsClaimKeyword(String text) {
        String lower = text.toLowerCase();
        for (String keyword : CLAIM_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsReviewTrigger(String text) {
        String lower = text.toLowerCase();
        for (String trigger : REVIEW_TRIGGERS) {
            if (lower.contains(trigger)) {
                return true;
            }
        }
        return false;
    }
}
