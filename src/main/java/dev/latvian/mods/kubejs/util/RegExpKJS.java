package dev.latvian.mods.kubejs.util;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.latvian.mods.kubejs.codec.KubeJSCodecUtil;
import dev.latvian.mods.rhino.regexp.NativeRegExp;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public interface RegExpKJS {
	Codec<Pattern> CODEC = KubeJSCodecUtil.stringResolverCodec(RegExpKJS::toRegExpString, RegExpKJS::wrap);
	StreamCodec<ByteBuf, Pattern> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(RegExpKJS::wrap, RegExpKJS::toRegExpString);

	@Nullable
	static Pattern wrap(Object o) {
		if (o instanceof CharSequence || o instanceof NativeRegExp) {
			return ofString(o.toString());
		} else if (o instanceof Pattern pattern) {
			return pattern;
		}

		return null;
	}

	static int getFlags(String f) {
		int flags = 0;

		for (var i = 0; i < f.length(); i++) {
			switch (f.charAt(i)) {
				case 'd' -> flags |= Pattern.UNIX_LINES;
				case 'i' -> flags |= Pattern.CASE_INSENSITIVE;
				case 'x' -> flags |= Pattern.COMMENTS;
				case 'm' -> flags |= Pattern.MULTILINE;
				case 's' -> flags |= Pattern.DOTALL;
				case 'u' -> flags |= Pattern.UNICODE_CASE;
				case 'U' -> flags |= Pattern.UNICODE_CHARACTER_CLASS;
			}
		}

		return flags;
	}

	static boolean isValidFlag(char c) {
		return c == 'd' || c == 'i' || c == 'x' || c == 'm' || c == 's' || c == 'u' || c == 'U';
	}

	@Nullable
	static Pattern ofString(String string) {
		if (string.length() < 3 || string.charAt(0) != '/') {
			return null;
		}

		return read(new StringReader(string));
	}

	static String toRegExpString(Pattern pattern) {
		var sb = new StringBuilder("/");
		sb.append(pattern.pattern());
		sb.append('/');

		var flags = pattern.flags();

		if ((flags & Pattern.UNIX_LINES) != 0) {
			sb.append('d');
		}

		if ((flags & Pattern.CASE_INSENSITIVE) != 0) {
			sb.append('i');
		}

		if ((flags & Pattern.COMMENTS) != 0) {
			sb.append('x');
		}

		if ((flags & Pattern.MULTILINE) != 0) {
			sb.append('m');
		}

		if ((flags & Pattern.DOTALL) != 0) {
			sb.append('s');
		}

		if ((flags & Pattern.UNICODE_CASE) != 0) {
			sb.append('u');
		}

		if ((flags & Pattern.UNICODE_CHARACTER_CLASS) != 0) {
			sb.append('U');
		}

		return sb.toString();
	}

	static Pattern read(StringReader reader) {
		if (!reader.canRead() || reader.peek() != '/') {
			throw new IllegalArgumentException("RegExp must start with /");
		}

		reader.skip();
		var pattern = new StringBuilder();

		while (reader.canRead()) {
			var c = reader.read();

			if (c == '\\' && reader.canRead() && reader.peek() == '/') {
				reader.skip();
				pattern.append('/');
			} else if (c == '/') {
				break;
			} else {
				pattern.append(c);
			}
		}

		var flags = new StringBuilder(0);

		while (reader.canRead() && isValidFlag(reader.peek())) {
			flags.append(reader.read());
		}

		return Pattern.compile(pattern.toString(), getFlags(flags.toString()));
	}

	static DataResult<Pattern> tryRead(StringReader reader) {
		try {
			return DataResult.success(read(reader));
		} catch (IllegalArgumentException ex) {
			return DataResult.error(() -> "Failed to parse regex from string: " + ex);
		}
	}
}
