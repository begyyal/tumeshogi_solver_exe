package it;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import begyyal.commons.object.collection.XGen;
import begyyal.shogi.TsSolver;
import begyyal.shogi.def.common.Player;
import begyyal.shogi.def.common.TsKoma;
import begyyal.shogi.entity.TsKihuRecord;
import begyyal.shogi.entity.TsMasuState;
import begyyal.shogi.entity.TsMotigomaState;

public class te11 {

    @Test
    public void p2() {

	long start = System.currentTimeMillis();
	int numOfMoves = 11;

	var ban = XGen.<TsMasuState>newHashSet();
	ban.add(new TsMasuState(Player.Gote, TsKoma.Ou, 1, 3));
	ban.add(new TsMasuState(Player.Gote, TsKoma.Kin, 4, 3));
	ban.add(new TsMasuState(Player.Gote, TsKoma.Uma, 1, 5));
	ban.add(new TsMasuState(Player.Sente, TsKoma.Hisya, 3, 4));

	var motigoma = XGen.<TsMotigomaState>newHashSet();
	motigoma.add(new TsMotigomaState(Player.Sente, TsKoma.Kaku, 1));
	motigoma.add(new TsMotigomaState(Player.Sente, TsKoma.Kin, 2));
	motigoma.add(new TsMotigomaState(Player.Sente, TsKoma.Keima, 1));
	motigoma.add(new TsMotigomaState(Player.Gote, TsKoma.Hu, 18));
	motigoma.add(new TsMotigomaState(Player.Gote, TsKoma.Kyousya, 4));
	motigoma.add(new TsMotigomaState(Player.Gote, TsKoma.Keima, 3));
	motigoma.add(new TsMotigomaState(Player.Gote, TsKoma.Gin, 4));
	motigoma.add(new TsMotigomaState(Player.Gote, TsKoma.Kin, 1));
	motigoma.add(new TsMotigomaState(Player.Gote, TsKoma.Hisya, 1));

	List<TsKihuRecord> rec = null;
	try (var solver = new TsSolver()) {
	    rec = solver.calculate(numOfMoves, ban, motigoma);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Error occured.");
	}

	Objects.requireNonNull(rec);
	assertEquals(rec.size(), numOfMoves);

	assertEquals(rec.get(0).toString(), "２５金");
	assertEquals(rec.get(1).toString(), "同玉");
	assertEquals(rec.get(2).toString(), "３５金");
	assertEquals(rec.get(3).toString(), "同玉");
	assertEquals(rec.get(4).toString(), "４５金");
	assertEquals(rec.get(5).toString(), "同玉");
	assertEquals(rec.get(6).toString(), "５５金");
	assertEquals(rec.get(7).toString(), "同玉");
	assertEquals(rec.get(8).toString(), "６５飛打");

	System.out.println("te11_2 - " + (System.currentTimeMillis() - start) + "msec");
    }
}
