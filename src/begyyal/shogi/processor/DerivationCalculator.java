package begyyal.shogi.processor;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import begyyal.commons.object.collection.XGen;
import begyyal.commons.object.collection.XList;
import begyyal.commons.util.cache.SimpleCacheResolver;
import begyyal.commons.util.function.XUtils;
import begyyal.shogi.constant.PublicCacheKey;
import begyyal.shogi.def.Koma;
import begyyal.shogi.object.Ban;
import begyyal.shogi.object.BanContext;

public class DerivationCalculator implements Closeable {

    private final int numOfMoves;
    private final BanContext origin;
    private final CalculationTools tools;

    public DerivationCalculator(
	int numOfMoves,
	Ban initBan,
	XList<Koma> selfMotigoma,
	XList<Koma> opponentMotigoma) {

	this.numOfMoves = numOfMoves;
	this.origin = new BanContext(selfMotigoma, opponentMotigoma);
	this.tools = new CalculationTools(numOfMoves, initBan);
    }

    public BanContext ignite() throws InterruptedException, ExecutionException {
	return this.r4spread(origin, this.tools.selfProcessor.spread(origin), 1);
    }

    private BanContext r4spread(BanContext context, BanContext[] branches, int count)
	throws InterruptedException, ExecutionException {

	if (branches == null || branches.length == 0) {
	    if (count % 2 == 0) {
		var s = context.getLatestState();
		return s.koma == Koma.Hu && s.utu ? null : context;
	    } else
		return null;
	} else if (count > numOfMoves)
	    return null;

	var futureMap = XGen.<BanContext, Future<BanContext[]>>newHashMap();
	for (var b : branches)
	    futureMap.put(b, this.tools.exe.submit(count % 2 == 0
		    ? () -> this.tools.selfProcessor.spread(b)
		    : () -> this.tools.opponentProcessor.spread(b)));

	BanContext result = null;
	int depth = 0;
	var i = futureMap.entrySet().stream()
	    .sorted((e1, e2) -> XUtils.compare(e1.getKey(), e2.getKey()))
	    .iterator();

	while (i.hasNext()) {

	    var e = i.next();
	    var k = e.getKey();

	    BanContext selected = SimpleCacheResolver.getAsPublic(PublicCacheKey.context, k.hash);
	    if (selected == null) {
		selected = r4spread(k, e.getValue().get(), count + 1);
		selected = selected == null ? BanContext.dummy : selected;
		SimpleCacheResolver.putAsPublic(PublicCacheKey.context, k.hash, selected);
	    }
	    if (selected == BanContext.dummy)
		selected = null;

	    if (count % 2 != 0) {
		if (selected != null && (result == null || result.log.size() > selected.log.size()))
		    result = selected;
	    } else if (selected == null) {
		return null;
	    } else if (depth < (depth = selected.log.size()))
		result = selected;
	}

	return result;
    }

    @Override
    public void close() throws IOException {
	this.tools.exe.shutdown();
    }

    private class CalculationTools {
	private final SelfProcessor selfProcessor;
	private final OpponentProcessor opponentProcessor;
	private final ExecutorService exe;

	CalculationTools(int numOfMoves, Ban initBan) {
	    this.selfProcessor = new SelfProcessor(numOfMoves, initBan);
	    this.opponentProcessor = new OpponentProcessor(numOfMoves);
	    this.exe = Executors.newCachedThreadPool();
	}
    }
}
