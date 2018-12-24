package marketSimulation;

import jade.core.AID;
import jade.core.Agent;
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

public class ProducerAgent extends Agent {

	private int msInterval = 100;
	// target of food to produce (amount of food)
	private int producingTarget = 0;
	// balance of money of producer
	private int moneyBalance = 0;
	// produced food by producer
    private ArrayList<Food> producedFood;
    // a list of known consumer agents
	private AID[] consumerAgents;
	// flag for printing verbose log
	private boolean verbose;


	// Put agent initializations here
	protected void setup() {
		producingTarget = Helpers.getRandomNumberBetweenOneAndOneHundred();

		// get strategy argument if specified, else use default strategy
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			verbose = Boolean.valueOf((String) args[0]);
		}
		print("Producer Agent " + getAID().getLocalName() + " is ready. Chosen parameters are " + producingTarget + " producing target, Verbose is " + verbose, true);

		producedFood = new ArrayList<>();

		for (int i = 0; i < producingTarget; i++) {
			producedFood.add(new Food());
		}

		// Register the player agent service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("producer-service");
		sd.setName("JADE-market-simulation");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
			print("Registered " + getAID().getLocalName() + "  as producer agent.", false);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new TickerBehaviour(this, msInterval) {
			protected void onTick() {
				// find all producer agents available and update list
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("consumer-service");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					consumerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						consumerAgents[i] = result[i].getName();
					}
					print("Found " + consumerAgents.length + " unique consumer agents.", false);
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}

				if(consumerAgents.length == 0) {
					doDelete();
				}
			}
		});

		addBehaviour(new FoodRequestResponseServer());
	}

	// agent takedown operations here
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		print("==================================================================", true);
		print(String.format("%-20s%-20s%-35s", "Producer Agent", "Money Balance", "Sold / Unsold / Total Food"), true);
		print("__________________________________________________________________", true);
		print(String.format("%-20s%-20d%-35s", getAID().getLocalName(), moneyBalance, (producingTarget-producedFood.size())+" / "+producedFood.size()+" / "+producingTarget), true);
		print("\n", true);
		print("==================================================================", true);
	}

	// print info to command line
	private void print(String toPrint, boolean always) {
		if(verbose || always){
			System.out.println(toPrint);
		}
	}

	private class FoodRequestResponseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				AID sender = msg.getSender();
				// result message received. Process it
				try {
					Food.Request foodRequest = (Food.Request) msg.getContentObject();

					print("Food request received from " + sender.getLocalName() + " => " + foodRequest.toString(), false);

					Food foodResponse = sellMatchingFood(foodRequest);

					ACLMessage message;

					if (foodResponse == null) {
						message = new ACLMessage(ACLMessage.DISCONFIRM);
					} else {
						message = new ACLMessage(ACLMessage.CONFIRM);
						message.setContentObject(foodResponse);
						print("Food sold to " + sender.getLocalName() + " => " + foodResponse.toString(), false);
					}

					message.addReceiver(sender);
					send(message);

					if(producedFood.isEmpty()) {
						doDelete();
					}

				} catch (UnreadableException | IOException e) {
					e.printStackTrace();
				}
			} else {
				block();
			}
		}

		private Food sellMatchingFood(Food.Request foodRequest) {
			Food foodResponse = null;

			for (Food food : producedFood) {
				if (food.getFoodType() == foodRequest.getFoodType()) {
					if(foodRequest.getBuyingStrategy() == Constants.BuyingStrategy.price) {
						if(food.getPrice() <= foodRequest.getMaxPrice()) {
							foodResponse = food;
							moneyBalance += food.getPrice();
							producedFood.remove(food);
							break;
						}
					} else if(foodRequest.getBuyingStrategy() == Constants.BuyingStrategy.quality) {
						if (food.getQuality() >= foodRequest.getMinQuality()) {
							foodResponse = food;
							moneyBalance += food.getPrice();
							producedFood.remove(food);
							break;
						}
					}
				}
			}
			return foodResponse;
		}
	}
}
