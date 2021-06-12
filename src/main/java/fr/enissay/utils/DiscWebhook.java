package fr.enissay.utils;

import fr.enissay.DiscordGen;
import fr.enissay.discord.DiscordWebhook;
import fr.enissay.proxy.ProxyVerifier;
import fr.enissay.runnable.GeneratorType;
import fr.enissay.runnable.Request;
import fr.enissay.runnable.RequestManager;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class DiscWebhook {

    public DiscWebhook(String URL, Request request){
        final DiscordWebhook webhook = new DiscordWebhook(URL);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        final String picLink = request.getGeneratorType() == GeneratorType.BOOST ? "https://cdn.discordapp.com/app-assets/521842831262875670/store/633877574094684160.webp?size=1024" : "https://cdn.discordapp.com/app-assets/521842831262875670/store/524691830454091797.webp?size=1024";

        //webhook.setContent("test");
        webhook.setTts(false);
        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("Generation #" + RequestManager.getRequests().size())
                .setDescription("A code has been generated")
                .setColor(request.getStatus().equalsIgnoreCase("VALID") ? Color.GREEN : Color.RED)
                .addField("Type", request.getGeneratorType().toString(), false)
                .addField("Status", request.getStatus(), false)
                .addField("Time", simpleDateFormat.format(request.getTime()), false)
                .addField("Current Delay", (DiscordGen.getReqDelay().get() / 1000L) + " seconds", false)
                .addField("Link", request.getLink(), false)
                .addField("Proxies", ProxyVerifier.getWorkingProxyList().size() + "", false)
                .setThumbnail(picLink)
                .setFooter(request.getGeneratorType().toString(), picLink)
                .setUrl(request.getLink())
                .setImage(picLink));
                /*.setAuthor("Author Name", "https://kryptongta.com", "https://kryptongta.com/images/kryptonlogowide.png")
                .setUrl("https://kryptongta.com"));*/
        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setDescription("Tries: " + DiscordGen.tries + " @ Valid: " + DiscordGen.valid + " @ Invalid: " + DiscordGen.invalid));
        try {
            webhook.execute(); //Handle exception
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
