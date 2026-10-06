package com.parasol;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmailGenerator {

    private static final int MAX_EMAILS = 15;
    private static final Random RANDOM = new Random();
    private final AtomicInteger sentCount = new AtomicInteger(0);

    private static final List<String> EMAILS = List.of(
            // Existing customers — general (RETENTION)
            "{\"from\":\"Sarah Mitchell <sarah.mitchell@email.com>\",\"subject\":\"Policy Renewal Question\",\"body\":\"Hi, I wanted to ask about renewing my auto policy. Can you send me the updated premium details?\"}",
            "{\"from\":\"Robert Chen <robert.chen@email.com>\",\"subject\":\"Billing Inquiry\",\"body\":\"I noticed a double charge on my last statement. Could you please review my account and issue a correction?\"}",
            "{\"from\":\"Emily Watson <emily.watson@email.com>\",\"subject\":\"Coverage Update Request\",\"body\":\"I recently added a home security system and would like to update my homeowner policy to reflect this change.\"}",
            "{\"from\":\"David Park <david.park@email.com>\",\"subject\":\"Payment Plan Options\",\"body\":\"I would like to switch from annual to monthly payments. What are the available payment plan options?\"}",
            "{\"from\":\"Michael Turner <michael.turner@email.com>\",\"subject\":\"Thank You Note\",\"body\":\"Just wanted to say thank you for the great service during my last call. Your team was very helpful.\"}",

            // New customers — general (ACQUISITION)
            "{\"from\":\"Lisa Chang <lisa.chang@email.com>\",\"subject\":\"Quote Request\",\"body\":\"I am looking for auto insurance for my new vehicle. Could you provide me with a quote?\"}",
            "{\"from\":\"Tom Bradley <tom.bradley@email.com>\",\"subject\":\"Coverage Comparison\",\"body\":\"I am comparing insurance providers. Can you send me details on your homeowner coverage plans?\"}",
            "{\"from\":\"Amanda Price <amanda.price@email.com>\",\"subject\":\"New Customer Inquiry\",\"body\":\"I was referred by a friend. What bundles do you offer for auto and home insurance together?\"}",

            // Existing customers — claim-related (CLAIMS)
            "{\"from\":\"Sarah Mitchell <sarah.mitchell@email.com>\",\"subject\":\"Fender Bender Report\",\"body\":\"I was involved in a minor collision in the parking lot this morning. No injuries but there is damage to my rear bumper.\"}",
            "{\"from\":\"Robert Chen <robert.chen@email.com>\",\"subject\":\"Garage Break-In\",\"body\":\"My garage was broken into last night and several items were stolen, including my bicycle and power tools.\"}",
            "{\"from\":\"James Rodriguez <james.rodriguez@email.com>\",\"subject\":\"Vehicle Theft\",\"body\":\"My car was stolen from the mall parking lot yesterday. I have filed a police report and need to start a claim.\"}",

            // New customers — claim-related (CLAIMS)
            "{\"from\":\"Tony Russo <tony.russo@email.com>\",\"subject\":\"Accident on Highway\",\"body\":\"I was in a multi-car accident on the highway. I have injuries and significant vehicle damage. I need to file a claim.\"}",
            "{\"from\":\"Wendy Brooks <wendy.brooks@email.com>\",\"subject\":\"Flood Damage\",\"body\":\"My basement flooded after the heavy rains last week. I have extensive water damage and loss of personal property.\"}",

            // Ambiguous emails — trigger REVIEW REQUIRED
            "{\"from\":\"Hank Morrison <hank.morrison@email.com>\",\"subject\":\"My car met a tree\",\"body\":\"So uh, my car and a pine tree had a disagreement on my way home last night. The tree won. The front end is... not great. Not sure what to do here, first time dealing with this kind of thing.\"}",
            "{\"from\":\"Diana Wells <diana.wells@email.com>\",\"subject\":\"Uninvited guests in my attic\",\"body\":\"Came home to find raccoons had torn through my roof and made themselves at home in the attic. There is insulation everywhere and the ceiling in my bedroom is sagging. Help?\"}",
            "{\"from\":\"Pete Nakamura <pete.nakamura@email.com>\",\"subject\":\"My porch just... left\",\"body\":\"Woke up this morning and half my front porch had collapsed. The support beams rotted through I guess. Nobody got hurt but I cannot use my front door now. What are my options?\"}",
            "{\"from\":\"Sylvia Okafor <sylvia.okafor@email.com>\",\"subject\":\"Neighbor kid vs my windshield\",\"body\":\"So the kid next door was practicing baseball and lets just say my windshield is now a spider web. I am not mad at the kid but I definitely need this sorted out.\"}",
            "{\"from\":\"Reggie Bloom <reggie.bloom@email.com>\",\"subject\":\"Something smells expensive\",\"body\":\"My furnace made a sound like a jet engine last night and now there is a crack running up my foundation wall that was not there before. The whole house shook. What do I do?\"}",
            "{\"from\":\"Claudia Voss <claudia.voss@email.com>\",\"subject\":\"My roof decided to become a skylight\",\"body\":\"There is a gaping hole where shingles used to be and I can literally see the sky from my living room couch. What now?\"}",
            "{\"from\":\"Darren Fitch <darren.fitch@email.com>\",\"subject\":\"The garage door had other plans\",\"body\":\"Came home and the garage door was crumpled like tinfoil on top of my wife's sedan. The spring mechanism gave out apparently. No idea what to do next.\"}",
            "{\"from\":\"Mariana Costa <mariana.costa@email.com>\",\"subject\":\"Found my fence three houses down\",\"body\":\"Last night's wind relocated my entire backyard fence into the neighbor's yard three doors over. Not sure how to even start dealing with this.\"}",
            "{\"from\":\"Owen Pratt <owen.pratt@email.com>\",\"subject\":\"Three AM wake-up call\",\"body\":\"A limb from the oak out back came through my bedroom window at three in the morning. Glass everywhere, rain coming in. Not sure how to proceed with all this.\"}",
            "{\"from\":\"Tamika Rhodes <tamika.rhodes@email.com>\",\"subject\":\"The ceiling is crying\",\"body\":\"Water is dripping through my kitchen ceiling and I have no idea where it is coming from. The plaster is bubbling and a chunk already fell into the sink. What should I do?\"}",
            "{\"from\":\"Jerome Katz <jerome.katz@email.com>\",\"subject\":\"Car vs shopping cart: a love story\",\"body\":\"A train of shopping carts rolled across the parking lot and straight into my driver side door. There is a lovely dent the size of a basketball. How do I handle this?\"}",
            "{\"from\":\"Lena Park-Kim <lena.parkkim@email.com>\",\"subject\":\"My living room has a new water feature\",\"body\":\"A pipe behind the wall decided to let go and now my hardwood floors are basically a wading pool. I turned off the main valve but the floors are warped. What are my options?\"}",
            "{\"from\":\"Brock Hensley <brock.hensley@email.com>\",\"subject\":\"Driveway ate my mailbox\",\"body\":\"The ground next to my driveway just gave way and swallowed the mailbox, a section of sidewalk, and part of the lawn. No idea where to start with this.\"}"
    );

    private final EmailRouter router;

    public EmailGenerator(EmailRouter router) {
        this.router = router;
    }

    @Scheduled(every = "45s", delayed = "5s")
    void generate() {
        if (sentCount.get() >= MAX_EMAILS) {
            return;
        }
        String email = EMAILS.get(RANDOM.nextInt(EMAILS.size()));
        router.consume(email);
        sentCount.incrementAndGet();
    }
}
