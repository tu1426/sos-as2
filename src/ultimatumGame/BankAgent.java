package ultimatumGame;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.MessageTemplate;

public class BankAgent extends Agent {
    // the amount of rounds that will be played by this bank agent
    private int msInterval = 5000;
	// the amount of rounds that will be played by this bank agent
	private int rounds = 10;
	// the amount of rounds that will be played by this bank agent
	private int roundCount = 0;
	// aggregated amount of money spread by this agent
	private int spreadMoney = 0;
	// aggregated amount of money that was actually spread (in meaning of accepted shares etc.)
	private int actuallySpreadMoney = 0;
	// the amount to spread per round
	private int minAmountToPlay = 100;
    // the amount to spread per round
    private int maxAmountToPlay = 1000;
	// a list of known player agents
	private AID[] playerAgents;


	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Bank Agent " + getAID().getName() + " is ready.");

		// get strategy argument if specified, else use default strategy
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			rounds = Integer.parseInt((String) args[0]);

			if (args.length > 1) {
                minAmountToPlay = Integer.parseInt((String) args[1]);
			}

            if (args.length > 2) {
                maxAmountToPlay = Integer.parseInt((String) args[2]);
            }

            if (args.length > 3) {
                msInterval = Integer.parseInt((String) args[3]);
            }
		}
		System.out.println("Chosen parameters are " + rounds + " rounds, a minAmount of " + minAmountToPlay + " and a maxAmount of " + maxAmountToPlay + " per round.");

		addBehaviour(new TickerBehaviour(this, msInterval) {
			protected void onTick() {
				// find all player agents available and update list
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("player-service");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					playerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						playerAgents[i] = result[i].getName();
					}
					System.out.println("Found " + playerAgents.length + " unique player agents.");
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}

				// Perform the request
				myAgent.addBehaviour(new SpreadMoneyBehaviour());
			}
		} );

		// add bank request behaviour
		addBehaviour(new SpreadResultServer());

	}

	// agent takedown operations here
	protected void takeDown() {
		// notify all player agents of bank agent shutdown
		ACLMessage cfp = new ACLMessage(ACLMessage.PROPAGATE);
		for (int i = 0; i < playerAgents.length; i++) {
			cfp.addReceiver(playerAgents[i]);
		}
		send(cfp);
		System.out.println("Bank Agent " + getAID().getName() + " terminating now. He spread " + actuallySpreadMoney + " out of " + spreadMoney + " (" +  String.format("%.2f", ((double) actuallySpreadMoney / (double) spreadMoney * 100)) + "%) in " + roundCount + " rounds.");
	}

	private class SpreadMoneyBehaviour extends OneShotBehaviour {
		public void action() {
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			int randomReceiver = (int) (Math.random() * playerAgents.length); // random int between 0 and size of playerAgents - 1
			cfp.addReceiver(playerAgents[randomReceiver]); // TODO: maybe some rules for selecting receiver?
            int randomAmount = (int) (Math.random() * (maxAmountToPlay - minAmountToPlay + 1)) + minAmountToPlay;
			cfp.setContent(String.valueOf(randomAmount));
			myAgent.send(cfp);
			System.out.println("Tried to spread " + randomAmount + " in round " + roundCount);
			roundCount ++;
			spreadMoney += randomAmount;
			if(roundCount == rounds){
				doDelete();
			}
		}
	}

	private class SpreadResultServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			int amountActuallySpread = 0;
			if (msg != null) {
				// result message received. Process it
				String result = msg.getContent();
				if(!result.equals("N")) {
					amountActuallySpread = Integer.parseInt(result);
					System.out.println("A deal worth of " + amountActuallySpread + " was completed.");
					actuallySpreadMoney += amountActuallySpread;
				}
			} else {
				block();
			}
		}
	}
}
