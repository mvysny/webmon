package sk.baka.webvm.analyzer;

import com.google.inject.ImplementedBy;
import java.util.List;

/**
 *
 * @author Martin Vysny
 */
@ImplementedBy(ProblemAnalyzer.class)
public interface IProblemAnalyzer {
    void configChanged();
    public List<ProblemReport> getProblems(List<HistorySample> history);
}
