package begyyal.shogi.processor;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import begyyal.commons.util.matrix.MatrixResolver;
import begyyal.shogi.def.Koma;
import begyyal.shogi.def.Player;
import begyyal.shogi.object.BanContext;
import begyyal.shogi.object.MasuState;

public class OpponentProcessor extends PlayerProcessorBase {

    public static final Player PlayerType = Player.Opponent;

    private OpponentProcessor() {
	super();
    }

    public BanContext[] spread(BanContext context) {

	var ban = context.getLatestBan();
	var opponentOu = ban
	    .search(s -> this.isOpponentOu(s))
	    .findFirst().get();

	// 王手範囲から避ける(王による王手駒の取得含む)
	var cs1 = spreadMasuState(opponentOu, ban)
	    .filter(s -> !s.rangedBy.anyMatch(s2 -> s2.player != PlayerType))
	    .map(s -> {
		var newBan = ban.clone();
		var k = newBan.advance(opponentOu, s);
		return context.branch(newBan, s, opponentOu, k, PlayerType, true);
	    });

	var outeArray = opponentOu.rangedBy
	    .stream()
	    .filter(s -> s.player != PlayerType)
	    .toArray(MasuState[]::new);
	if (outeArray.length > 1)
	    return cs1.toArray(BanContext[]::new);

	// 王手駒を取得する(王による取得は含まず)
	var outeState = outeArray[0];
	var cs2 = outeState.rangedBy
	    .stream()
	    .filter(s -> s.player == PlayerType && s.koma != Koma.Ou)
	    .map(s -> Pair.of(s, this.occupy(s, outeState)))
	    .map(sp -> {
		var to = sp.getRight();
		var newBan = ban.clone();
		var k = newBan.advance(sp.getLeft(), to);
		return newBan.validateState(to)
			? context.branch(newBan, to, sp.getLeft(), k, PlayerType, true)
			: null;
	    })
	    .filter(c -> c != null);

	// 持ち駒を貼る
	var outeVector = opponentOu.getVectorTo(outeState);
	var cs3 = context.opponentMotigoma.isEmpty() || outeVector.x() < 2 && outeVector.y() < 2
		? Stream.empty()
		: Arrays.stream(MatrixResolver.decompose(outeVector))
		    .filter(miniV -> miniV != outeVector)
		    .flatMap(v -> context.opponentMotigoma
			.stream()
			.distinct()
			.map(k -> {
			    var newBan = ban.clone();
			    int x = opponentOu.x + v.x(), y = opponentOu.y + v.y();
			    var s = newBan.deploy(k, x, y, PlayerType);
			    return s == null ? null
				    : context.branch(
					newBan, s, ban.getState(x, y), k, PlayerType, false);
			}))
		    .filter(c -> c != null);

	// selfに同じく駒移動系は重複し得るのでdistinctする
	return Stream.concat(Stream.concat(cs1, cs2).distinct(), cs3)
	    .toArray(BanContext[]::new);
    }

    @Override
    protected Player getPlayerType() {
	return PlayerType;
    }

    public static OpponentProcessor newi() {
	return new OpponentProcessor();
    }
}
