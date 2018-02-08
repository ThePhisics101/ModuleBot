package modulebot.main;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import modulebot.main.cmds.*;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class Main extends ListenerAdapter implements CommandHost {
    public static HashMap<Long, String> prefix = new HashMap<>();
    public static HashMap<Long, HashMap<String, ArrayList<String>>> settings = new HashMap<>(); // users, roles, etc
    public static HashMap<String, Command[]> modules = new HashMap<>();
    private static HashMap<Long, LinkedHashMap<String, String>> messages = new HashMap<>();

    public static Connection conn;

    public static void main(String[] args) throws LoginException, InterruptedException, InstantiationException, IllegalAccessException {
        if (args.length < 5) return;
        new JDABuilder(AccountType.BOT).setToken(args[0]).buildBlocking().addEventListener(
                new Main(args[1], args[2], args[3], args[4])
        );
    }

    private static void openDB(String user, String pass, String name, String server) throws SQLException {
        MysqlDataSource source = new MysqlDataSource();
        source.setUser(user);
        source.setPassword(pass);
        source.setDatabaseName(name);
        source.setServerName(server);
        conn = source.getConnection();
    }
    private static void closeDB() throws SQLException {
        if (conn != null) conn.close();
    }

    @Override
    public Command[] getCommands() {
        return new Command[] {
                new Help(),
                new Ping(),
                new Prefix(),
                new Permissions(),
                new Moudule()
        };
    }

    @Override
    public String getName() { return "main"; }

    @Override
    public String getDescription() { return "Hosts main commands"; }

    private Main(String user, String pass, String name, String server) throws IllegalAccessException, InstantiationException {
        try {
            Main.openDB(user, pass, name, server);
            Statement st = Main.conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM servers");
            while (rs.next()) {
                long id = rs.getLong("id");
                prefix.put(id, rs.getString("prefix"));
                HashMap<String, ArrayList<String>> map = new HashMap<>();
                String modules  = rs.getString("modules" );
                String banUsers = rs.getString("banUsers");
                String banRoles = rs.getString("banRoles");
                String admUsers = rs.getString("admUsers");
                String admRoles = rs.getString("admRoles");
                map.put("modules" , new ArrayList<>(Arrays.asList((modules  == null ? "main" : modules ).split(";"))));
                map.put("banUsers", new ArrayList<>(Arrays.asList((banUsers == null ? ""     : banUsers).split(";"))));
                map.put("banRoles", new ArrayList<>(Arrays.asList((banRoles == null ? ""     : banRoles).split(";"))));
                map.put("admUsers", new ArrayList<>(Arrays.asList((admUsers == null ? ""     : admUsers).split(";"))));
                map.put("admRoles", new ArrayList<>(Arrays.asList((admRoles == null ? ""     : admRoles).split(";"))));
                settings.put(id, map);
                messages.put(id, new LinkedHashMap<String, String>() {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > 10;
                    }
                });
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        CommandHost[] modules1 = new CommandHost[] {
                this,
                new modulebot.info.Main()
        };
        for (CommandHost module : modules1) {
            modules.put(module.getName(), module.getCommands());
        }
        /*Reflections reflections = new Reflections("modulebot");
        Set<Class<? extends CommandHost>> classes = reflections.getSubTypesOf(CommandHost.class);
        for (Class<? extends CommandHost> c : classes) {
            CommandHost ch = c.newInstance();
            modules.put(ch.getName(), ch.getCommands());
        }*/
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        SelfUser su = event.getJDA().getSelfUser();
        long gid = event.getGuild().getIdLong();
        String prefix = Main.prefix.get(gid);

        if (!content.startsWith(prefix) && !content.startsWith(su.getAsMention())) return;
        if (settings.get(gid).get("banUsers").contains(event.getAuthor().getId())) return;
        for (Role r : event.getMember().getRoles())
            if (settings.get(gid).get("banRoles").contains(r.getId())) return;

        String[] cmds = content.substring(content.startsWith(prefix) ? prefix.length() : su.getAsMention().length())
                .trim().toLowerCase().split(" ");
        for (int i = 0; i < cmds.length; i++) cmds[i] = cmds[i].trim();
        boolean admin = false;

        if (settings.get(gid).get("admUsers").contains(event.getAuthor().getId())) admin = true;
        for (Role r : event.getMember().getRoles())
            if (settings.get(gid).get("admRoles").contains(r.getId())) admin = true;

        Command c = getCommand(cmds, gid);
        if (c != null) {
            messages.get(gid).put(event.getMessageId(), c.execute(event.getMessage(), admin, null));
        } else {
            messages.get(gid).put(event.getMessageId(),
                    event.getChannel().sendMessage("Command not found").complete().getId()
            );
        }
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        String content = event.getMessage().getContentRaw();
        SelfUser su = event.getJDA().getSelfUser();
        long gid = event.getGuild().getIdLong();
        String prefix = Main.prefix.get(gid);

        if (!content.startsWith(prefix) && !content.startsWith(su.getAsMention())) return;
        if (settings.get(gid).get("banUsers").contains(event.getAuthor().getId())) return;
        for (Role r : event.getMember().getRoles())
            if (settings.get(gid).get("banRoles").contains(r.getId())) return;

        content = content.substring(content.startsWith(prefix) ? prefix.length() : su.getAsMention().length()).trim();
        boolean admin = false;

        if (settings.get(gid).get("admUsers").contains(event.getAuthor().getId())) admin = true;
        for (Role r : event.getMember().getRoles())
            if (settings.get(gid).get("admRoles").contains(r.getId())) admin = true;

        String[] cmds = content.toLowerCase().split(" ");

        Command c = getCommand(cmds, gid);
        if (c != null) {
            c.execute(event.getMessage(), admin, messages.get(gid).get(event.getMessageId()));
        } else {
            event.getChannel().editMessageById(messages.get(gid).get(event.getMessageId()),
                    "Command not found"
            ).queue();
        }
    }

    private static Command getCommand(String[] cmds, long gid) {
        ArrayList<String> modulesS = Main.settings.get(gid).get("modules");
        if (cmds.length > 1) for (String module : modulesS) if (module.equals(cmds[0]))
            for (Command c : modules.get(module)) if (c.getName().equals(cmds[1])) return c.cloneCMD();
        Command cmd = null;
        for (String module : Main.settings.get(gid).get("modules"))
            for (Command c : modules.get(module))
                if (c.getName().equals(cmds[0])) if (cmd == null) cmd = c.cloneCMD(); else return null;
        for (Command c : modules.get("main")) if (c.getName().equals(cmds[0])) return c.cloneCMD();
        return cmd;
    }

    @Override
    public void onDisconnect(DisconnectEvent event) {
        try {
            Main.closeDB();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}