package ethanjones.cubes.side.server.command;

import ethanjones.cubes.core.localization.Localization;
import ethanjones.cubes.networking.server.ClientIdentifier;
import ethanjones.cubes.side.common.Cubes;

public abstract class CommandValue<T> {

  public static final CommandValue<String> stringValue = new CommandValue<String>() {
    @Override
    public String getArgument(String string) throws CommandParsingException {
      return string;
    }

    @Override
    public String toString() {
      return Localization.get("command.common.value.stringValue.string");
    }
  };

  public static final CommandValue<Float> floatValue = new CommandValue<Float>() {
    @Override
    public Float getArgument(String string) throws CommandParsingException {
      try {
        return Float.parseFloat(string);
      } catch (NumberFormatException e) {
        throw new CommandParsingException("command.common.value.floatValue.parsing");
      }
    }

    @Override
    public String toString() {
      return Localization.get("command.common.value.floatValue.string");
    }
  };

  public static final CommandValue<Integer> intValue = new CommandValue<Integer>() {
    @Override
    public Integer getArgument(String string) throws CommandParsingException {
      try {
        return Integer.parseInt(string);
      } catch (NumberFormatException e) {
        throw new CommandParsingException("command.common.value.intValue.parsing");
      }
    }

    @Override
    public String toString() {
      return Localization.get("command.common.value.intValue.string");
    }
  };

  public static final CommandValue<Float> coordinate = new CommandValue<Float>() {
    @Override
    public Float getArgument(String string) throws CommandParsingException {
      try {
        return Float.parseFloat(string);
      } catch (NumberFormatException e) {
        throw new CommandParsingException("command.common.value.coordinate.parsing");
      }
    }

    @Override
    public String toString() {
      return Localization.get("command.common.value.coordinate.string");
    }
  };

  public static final CommandValue<Integer> blockCoordinate = new CommandValue<Integer>() {
    @Override
    public Integer getArgument(String string) throws CommandParsingException {
      try {
        return Integer.parseInt(string);
      } catch (NumberFormatException e) {
        throw new CommandParsingException("command.common.value.blockCoordinate.parsing");
      }
    }

    @Override
    public String toString() {
      return Localization.get("command.common.value.blockCoordinate.string");
    }
  };

  public static final CommandValue<ClientIdentifier> clientIdentifier = new CommandValue<ClientIdentifier>() {
    @Override
    public ClientIdentifier getArgument(String string) throws CommandParsingException {
      ClientIdentifier client = Cubes.getServer().getClient(string);
      if (client == null) throw new CommandParsingException("commands.common.value.clientIdentifier.parsing");
      return client;
    }

    @Override
    public String toString() {
      return Localization.get("command.common.value.clientIdentifier.string");
    }
  };

  public static final CommandValue<CommandBuilder> command = new CommandValue<CommandBuilder>() {
    @Override
    public CommandBuilder getArgument(String string) throws CommandParsingException {
      CommandBuilder commandBuilder = CommandManager.commands.get(string);
      if (commandBuilder == null) throw new CommandParsingException("command.common.value.command.parsing");
      return commandBuilder;
    }

    @Override
    public String toString() {
      return Localization.get("command.common.value.command.string");
    }
  };

  public abstract T getArgument(String string) throws CommandParsingException;

  public abstract String toString();

}
