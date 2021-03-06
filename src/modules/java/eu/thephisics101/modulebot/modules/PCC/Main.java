package eu.thephisics101.modulebot.modules.PCC;

import eu.thephisics101.modulebot.hosts.Command;
import eu.thephisics101.modulebot.hosts.CommandHost;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;

import java.sql.PreparedStatement;

public class Main extends CommandHost {
    @Override
    public Command[] getCommands() {
        return new Command[] {new Roles()};
    }

    @Override
    public String getName() {
        return "pcc-roles";
    }

    @Override
    public String getDescription() {
        return "Specialised command for Programming Chit Chat";
    }

    @Override
    public void onEnabled(long gid, TextChannel c) {
        if (gid != 140541822266114048L) {
            try {
                eu.thephisics101.modulebot.Main.settings.get(gid).get("modules").remove("pcc-roles");
                PreparedStatement st = eu.thephisics101.modulebot.Main.conn.prepareStatement("UPDATE servers SET modules = ? WHERE id = ?");
                st.setString(1, String.join(";", eu.thephisics101.modulebot.Main.settings.get(gid).get("modules")));
                st.setLong(2, gid);
                st.executeUpdate();
                st.close();
                if (c != null)
                    c.sendMessage("Module \"pcc-roles\" is meant only for Programming Chit Chat (<https://discord.gg/0mfgHl7u5FsrBJho>), so it was automatically disabled").queue();

                for (String n : eu.thephisics101.modulebot.Main.settings.get(gid).get("modules")) {
                    if (eu.thephisics101.modulebot.Main.commandHosts.containsKey(n)) {
                        eu.thephisics101.modulebot.Main.commandHosts.get(n).onDisabled(gid, c);
                        eu.thephisics101.modulebot.Main.commandHosts.get(n).onToggled(gid, c);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getMember().getUser().isBot()) {
            event.getGuild().getController().addRolesToMember(event.getMember(), event.getGuild().getRoleById("213312815954526208")).queue();
        } else {
            event.getGuild().getController().addRolesToMember(event.getMember(), event.getGuild().getRoleById("145578521702432768")).queue();
        }
    }
}
