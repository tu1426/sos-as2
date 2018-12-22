package marketSimulation;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;

public class ConsumerAgent extends Agent {

	// the amount of rounds that will be played by this bank agent
	private int msInterval = 5000;
	// the strategy the agent will follow
	private Constants.BuyingStrategy buyingStrategy;
	// actual amount of money
	private int moneyBalance;
	// amount of money at the beginning
	private int moneyAtStart;
	// target of food to buy (amount of food)
	private int buyingTarget;
	// bought food from producer
	private ArrayList<Food> boughtFood;
	// a list of known producer agents
	private AID[] producerAgents;
	// flag for printing verbose log
	private boolean verbose;
	// flag for indicating termination
	private boolean terminating = false;


	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		print("Consumer Agent " + getAID().getName() + " is ready.", true);

		buyingStrategy = Constants.BuyingStrategy.values()[(int) (Math.random() * 10) % 2];

		buyingTarget = ((int) (Math.random() * 10)) + 1;

		moneyBalance = ((int) (Math.random() * 100)) + 1;
		moneyAtStart = moneyBalance;

		// get strategy argument if specified, else use default strategy
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			verbose = Boolean.parseBoolean((String) args[0]);
		}
		print("Chosen strategy is " + buyingStrategy + ", buying target is " + buyingTarget + ", money balance is " + moneyBalance + ", verbose is " + verbose + ".", true);

		addBehaviour(new TickerBehaviour(this, msInterval) {
			protected void onTick() {
				if(!terminating) {
					// find all producer agents available and update list
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("producer-service");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						producerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							producerAgents[i] = result[i].getName();
						}
						print("Found " + producerAgents.length + " unique producer agents.");
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}

					myAgent.addBehaviour(new BuyRequestClient());
					myAgent.addBehaviour(new BuyResponseClient());
				}
			}
		} );
	}

	// agent takedown operations here
	protected void takeDown() {
		print("==============================================================================", true);
		print(String.format("%-15s%-15s%-15s%-15s%-15s%-15s", "Consumer Agent", "Strategy", "Buying Target",  "Money at Beginning", "Actual Money Balance", "Bought Food"), true);
		print(String.format("%-15s%-15s%-15d%-15d%-15d%-15d", getAID().getLocalName(), buyingStrategy, buyingTarget, moneyAtStart, moneyBalance, boughtFood.size()), true);
		print("______________________________________________________________________________", true);
	}

	// print info to command line
	private void print(String toPrint, boolean always) {
		if(verbose || always){
			System.out.println(toPrint);
		}
	}

	// print info to command line
	private void print(String toPrint) {
		print(toPrint, false);
	}

	private class BuyRequestClient extends CyclicBehaviour {
		public void action() {
			ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
			int randomReceiver = (int) (Math.random() * producerAgents.length); // random int between 0 and size of producerAgents - 1
			message.addReceiver(producerAgents[randomReceiver]);

			FoodRequest foodRequest = createFoodRequest();

			try {
				message.setContentObject(foodRequest);
				myAgent.send(message);
				print("Food request sent: type - " + foodRequest.getFoodType() + ", minimum quality - " +foodRequest.getMinQuality() + ", maximum price - " + foodRequest.getMaxPrice() +";", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private FoodRequest createFoodRequest() {
			FoodRequest foodRequest = null;

			// TODO implement method

			return foodRequest;
		}
	}

	private class BuyResponseClient extends CyclicBehaviour {
		public void action() {

		}
	}
}
