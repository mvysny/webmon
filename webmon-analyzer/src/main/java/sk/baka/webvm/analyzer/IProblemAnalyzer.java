package sk.baka.webvm.analyzer;

import java.util.List;
import sk.baka.webvm.analyzer.config.Config;

/**
 *
 * @author Martin Vysny
 */
public interface IProblemAnalyzer {
    void configChanged(Config config);
    public List<ProblemReport> getProblems(List<HistorySample> history);
}
