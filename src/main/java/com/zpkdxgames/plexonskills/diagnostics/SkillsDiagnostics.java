package com.zpkdxgames.plexonskills.diagnostics;

import java.util.concurrent.atomic.LongAdder;

public final class SkillsDiagnostics {
    private final LongAdder xpGrants = new LongAdder();
    private final LongAdder xpRejected = new LongAdder();
    private final LongAdder originRejected = new LongAdder();
    private final LongAdder profileNotReady = new LongAdder();
    private final LongAdder levelUps = new LongAdder();
    private final LongAdder blockFacts = new LongAdder();
    private final LongAdder combatFacts = new LongAdder();
    private final LongAdder fishingFacts = new LongAdder();
    private final LongAdder acrobaticsFacts = new LongAdder();
    private final LongAdder persistenceFailures = new LongAdder();
    private final LongAdder flushBatches = new LongAdder();
    private final LongAdder shadowContributions = new LongAdder();
    private final LongAdder abilityActivations = new LongAdder();
    private final LongAdder abilityRejected = new LongAdder();
    private final LongAdder abilityEnded = new LongAdder();
    private final LongAdder milestonesReached = new LongAdder();

    public void xpGrant(){xpGrants.increment();} public void xpRejected(){xpRejected.increment();}
    public void originRejected(){originRejected.increment();} public void profileNotReady(){profileNotReady.increment();}
    public void levelUp(){levelUps.increment();} public void blockFact(){blockFacts.increment();}
    public void combatFact(){combatFacts.increment();} public void fishingFact(){fishingFacts.increment();}
    public void acrobaticsFact(){acrobaticsFacts.increment();} public void persistenceFailure(){persistenceFailures.increment();}
    public void flushBatch(){flushBatches.increment();} public void shadowContribution(){shadowContributions.increment();}
    public void abilityActivation(){abilityActivations.increment();} public void abilityRejected(){abilityRejected.increment();}
    public void abilityEnded(){abilityEnded.increment();} public void milestoneReached(){milestonesReached.increment();}

    public Snapshot snapshot(){
        return new Snapshot(
            xpGrants.sum(),xpRejected.sum(),originRejected.sum(),profileNotReady.sum(),levelUps.sum(),
            blockFacts.sum(),combatFacts.sum(),fishingFacts.sum(),acrobaticsFacts.sum(),persistenceFailures.sum(),
            flushBatches.sum(),shadowContributions.sum(),abilityActivations.sum(),abilityRejected.sum(),abilityEnded.sum(),milestonesReached.sum());
    }

    public record Snapshot(
        long xpGrants,long xpRejected,long originRejected,long profileNotReady,long levelUps,
        long blockFacts,long combatFacts,long fishingFacts,long acrobaticsFacts,long persistenceFailures,
        long flushBatches,long shadowContributions,long abilityActivations,long abilityRejected,long abilityEnded,long milestonesReached
    ){}
}
