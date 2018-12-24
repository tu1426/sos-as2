package marketSimulation;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
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

	private int msInterval = 100;
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


	// Put agent initializations here
	protected void setup() {
		buyingStrategy = Helpers.getRandomBuyingStrategy();

		buyingTarget = Helpers.getRandomNumberBetweenOneAndTen();

		moneyBalance = Helpers.getRandomNumberBetweenOneAndOneHundred();
		moneyAtStart = moneyBalance;

		boughtFood = new ArrayList<>();

		// get strategy argument if specified, else use default strategy
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			verbose = Boolean.parseBoolean((String) args[0]);
		}
		print("Consumer Agent '" + getAID().getLocalName() + "' is ready. Chosen strategy is " + buyingStrategy + ", buying target is " + buyingTarget + ", money balance is " + moneyBalance + ", verbose is " + verbose + ".", true);

		// Register the player agent service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("consumer-service");
		sd.setName("JADE-market-simulation");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
			print("Registered " + getAID().getLocalName() + "  as consumer agent.", false);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new TickerBehaviour(this, msInterval) {
			protected void onTick() {
				// if money is less than 10 it is not guaranteed that consumer can pay for food (there is actually no acknowledgement back to producer)
				if(boughtFood.size() < buyingTarget && moneyBalance >= 10) {
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
						print("Found " + producerAgents.length + " unique producer agents.", false);
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}

					if(producerAgents.length == 0) {
						doDelete();
					}

					myAgent.addBehaviour(new RequestResponseClient());
				} else {
					doDelete();
				}
			}
		});
	}

	// agent takedown operations here
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		print("===========================================================================================", true);
		print(String.format("%-20s%-10s%-15s%-20s%-15s%-15s", "Consumer Agent", "Strategy", "Buying Target",  "Money Balance @ T0", "Money Balance", "Bought Food"), true);
		print("___________________________________________________________________________________________", true);
		print(String.format("%-20s%-10s%-15d%-20d%-15d%-15d", getAID().getLocalName(), buyingStrategy, buyingTarget, moneyAtStart, moneyBalance, boughtFood.size()), true);
		print("===========================================================================================", true);
	}

	// print info to command line
	private void print(String toPrint, boolean always) {
		if(verbose || always){
			System.out.println(toPrint);
		}
	}

	private class RequestResponseClient extends Behaviour {
		private int step = 0;

		public void action() {
			switch (step) {
				case 0:
					// if money is less than 10 it is not guaranteed that consumer can pay for food (there is actually no acknowledgement back to producer)
					if (boughtFood.size() < buyingTarget && moneyBalance >= 10) {
						// send the food request to producer
						ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
						int randomReceiver = (int) (Math.random() * producerAgents.length); // random int between 0 and size of producerAgents - 1
						message.addReceiver(producerAgents[randomReceiver]);

						Food.Request foodRequest = createFoodRequest();

						try {
							message.setContentObject(foodRequest);
							myAgent.send(message);
							print("Food request sent to " + producerAgents[randomReceiver].getLocalName() + " => " + foodRequest.toString(), false);
						} catch (IOException e) {
							e.printStackTrace();
						}
						step = 1;
					} else {
						doDelete();
					}
					break;
				case 1:
					// if money is less than 10 it is not guaranteed that consumer can pay for food (there is actually no acknowledgement back to producer)
					if (boughtFood.size() < buyingTarget && moneyBalance >= 10) {
						// Receive response for proposed share
						MessageTemplate mtConfirm = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
						MessageTemplate mtDisconfirm = MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM);
						ACLMessage msgConfirm = myAgent.receive(mtConfirm);
						ACLMessage msgDisonfirm = myAgent.receive(mtDisconfirm);

						if (msgConfirm != null) {
							AID sender = msgConfirm.getSender();
							// result message received. Process it
							try {
								Food foodResponse = (Food) msgConfirm.getContentObject();
								boughtFood.add(foodResponse);
								moneyBalance -= foodResponse.getPrice();
								step = 2;
								print("Food bought from " + sender.getLocalName() + " => " + foodResponse.toString(), false);
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
						}

						if(msgDisonfirm != null) {
							step = 2;
						}

						if (msgConfirm == null && msgDisonfirm == null) {
							block();
						}
					} else {
						doDelete();
					}
					break;
			}
		}

		private Food.Request createFoodRequest() {
			Constants.FoodType foodType = Helpers.getRandomFoodType();

			int minQuality = 0;
			int maxPrice = 0;

			if(buyingStrategy == Constants.BuyingStrategy.price) {
				maxPrice = Helpers.getRandomNumberBetweenOneAndFive();
			} else if(buyingStrategy == Constants.BuyingStrategy.quality) {
				minQuality = Helpers.getRandomNumberBetweenThreeAndFive();
			}
			return new Food.Request(foodType, maxPrice, minQuality, buyingStrategy);
		}

		public boolean done() {
			return boughtFood.size() >= buyingTarget || moneyBalance < 10 || step == 2;
		}
	}
}
