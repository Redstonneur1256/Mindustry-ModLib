package fr.redstonneur1256.modlib.events.net.server;

import arc.Core;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.game.Gamemode;
import mindustry.gen.Groups;
import mindustry.net.Administration;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import static mindustry.Vars.charset;

/**
 * Event called when a user ping the Mindustry server, all fields lengths limit have been removed however
 * it will break if the combined length of the fields versionType, name, description, map and custom gamemode
 * exceeds 478 bytes or if a single string is more than 256 bytes
 */
public class ServerListPingEvent {

    /**
     * The address that pinged the server
     */
    public InetAddress address;
    /**
     * Should the server appear offline ?
     * If true the server will appear as offline and no reply will be sent to the ping
     */
    public boolean offline;

    /**
     * Game build
     */
    public int versionBuild;
    /**
     * Game version (official/custom)
     */
    public String versionType;

    /**
     * Server name
     */
    public String name;
    /**
     * Server description
     */
    public String description;
    /**
     * Current server map
     */
    public String map;
    /**
     * Current game wave
     */
    public int wave;
    /**
     * Server gamemode
     */
    public Gamemode gamemode;
    /**
     * Custom game mode to display instead of gamemode
     */
    public String customGamemode;

    /**
     * Current connected player count
     */
    public int playerCount;
    /**
     * Player limit count or -1 to disable
     */
    public int playerLimit;

    public ServerListPingEvent() {

    }

    /**
     * Applies the default mindustry values to the event
     */
    public void setDefaults() {
        versionBuild = Version.build;
        versionType = Version.type;

        name = Vars.headless ? Administration.Config.serverName.string() : Vars.player.name;
        description = Vars.headless && !Administration.Config.desc.string().equals("off") ? Administration.Config.desc.string() : "";
        map = Vars.state.map.name();
        wave = Vars.state.wave;
        gamemode = Vars.state.rules.mode();
        customGamemode = Vars.state.rules.modeName;

        playerCount = Core.settings.getInt("totalPlayers", Groups.player.size());
        playerLimit = Vars.netServer.admins.getPlayerLimit();
    }

    /**
     * Writes the server data to a new ByteBuffer
     *
     * @return the encoded server data
     */
    public ByteBuffer writeServerData() {
        ByteBuffer buffer = ByteBuffer.allocate(500);

        writeString(buffer, name);
        writeString(buffer, map);

        buffer.putInt(playerCount);
        buffer.putInt(wave);

        buffer.putInt(versionBuild);
        writeString(buffer, versionType);

        buffer.put((byte) gamemode.ordinal());
        buffer.putInt(playerLimit);

        writeString(buffer, description);
        if (customGamemode != null) {
            writeString(buffer, customGamemode);
        }

        return buffer;
    }

    private static void writeString(ByteBuffer buffer, String string) {
        byte[] bytes = string.getBytes(charset);
        buffer.put((byte) bytes.length);
        buffer.put(bytes);
    }

}
