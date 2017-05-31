/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.parameters;

import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.command.CommandBase.getListOfStringsMatchingLastWord;

/**
 * Created by lukas on 30.05.17.
 */
public class Expect
{
    protected final Map<String, Param> params = new HashMap<>();

    protected String cur;

    private Expect()
    {

    }

    public static Expect start()
    {
        return new Expect();
    }

    public Expect structurePredicate()
    {
        return structure();
    }

    public Expect structure()
    {
        return next(StructureRegistry.INSTANCE.ids());
    }

    public Expect pos(@Nullable BlockPos pos)
    {
        return next(args -> CommandBase.getTabCompletionCoordinate(args, 0, pos))
                .next(args -> CommandBase.getTabCompletionCoordinate(args, 1, pos))
                .next(args -> CommandBase.getTabCompletionCoordinate(args, 2, pos));
    }

    public Expect surfacePos(@Nullable BlockPos pos)
    {
        return next(args -> CommandBase.getTabCompletionCoordinateXZ(args, 0, pos))
                .next(args -> CommandBase.getTabCompletionCoordinateXZ(args, 1, pos));
    }

    public Expect rotation()
    {
        return next(args -> getListOfStringsMatchingLastWord(args, "0", "1", "2", "3"));
    }

    public Expect mirror()
    {
        return next(args -> getListOfStringsMatchingLastWord(args, "false", "true"));
    }

    public Expect dimension()
    {
        return next(args -> getListOfStringsMatchingLastWord(args, Arrays.stream(DimensionManager.getIDs()).map(String::valueOf).collect(Collectors.toList())));
    }

    public Expect named(String name)
    {
        cur = name;
        return this;
    }

    public Expect next(Completer completion)
    {
        Param cur = params.get(this.cur);
        if (cur == null)
            params.put(this.cur, cur = new Param());
        cur.next(completion);
        return this;
    }

    public Expect next(Collection<? extends String> completion)
    {
        return next((server, sender, args, pos) -> getListOfStringsMatchingLastWord(args, completion));
    }

    public Expect next(Function<String[], List<String>> completion)
    {
        return next((server, sender, args, pos) -> completion.apply(args));
    }

    public List<String> get(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        List<String> params = Arrays.asList(Parameters.quoted(args));
        String[] paramArray = params.toArray(new String[params.size()]);

        Set<String> flags = new HashSet<>();
        int curIndex = 0;

        String curName = null;
        for (int i = 0; i < params.size() - 1; i++)
        {
            String param = params.get(i);
            curIndex++;

            if (param.startsWith("-"))
            {
                flags.add(curName = param.substring(1));
                curIndex = 0;
            }
        }

        Param param = this.params.get(curName);

        if (param == null || curIndex >= param.completion.size())
            return getListOfStringsMatchingLastWord(paramArray, this.params.keySet().stream()
                    .filter(p -> p != null && !flags.contains(p))
                    .map(p -> "-" + p).collect(Collectors.toList()));

        return param.completion.get(curIndex).complete(server, sender, paramArray, pos).stream()
                // More than one word, let's wrap this in quotes
                .map(s -> s.contains(" ") && !s.startsWith("\"") ? String.format("\"%s\"", s) : s)
                .collect(Collectors.toList());
    }

    public interface Completer
    {
        public List<String> complete(MinecraftServer server, ICommandSender sender, String[] argss, @Nullable BlockPos pos);
    }

    public class Param
    {
        private final List<Completer> completion = new ArrayList<>();

        public Param next(Completer completion)
        {
            this.completion.add(completion);
            return this;
        }
    }
}