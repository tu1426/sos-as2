package marketSimulation;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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

	// target of food to produce (amount of food)
	private int producingTarget = 0;
	// balance of money of producer
	private int moneyBalance = 0;
	// produced food by producer
    private ArrayList<Food> producedFood;
	// flag for printing verbose log
	private boolean verbose;


	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		print("Producer Agent " + getAID().getName() + " is ready.", true);

		producingTarget = ((int) (Math.random() * 100)) + 1;

		// get strategy argument if specified, else use default strategy
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			verbose = Boolean.valueOf((String) args[0]);
		}
		print("Chosen parameters are " + producingTarget + " producing target, Verbose is " + verbose, true);

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
			print("Registered " + getAID().getName() + "  as producer agent.");
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// add bank request behaviour
		addBehaviour(new BuyRequestResponseServer());
	}

	// agent takedown operations here
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		print("==============================================================================", true);
		print(String.format("%20s%-20s%-20s%-25s", "Producer Agent", "Money Balance", "Total Food", "Sold / Unsold Food"), true);
		print("______________________________________________________________________________", true);
		print(String.format("%-20s%-20d%-20d%-25s", getAID().getLocalName(), moneyBalance, producingTarget, (producingTarget-producedFood.size())+" / "+producedFood.size()), true);
		print("\n", true);
		print("==============================================================================", true);
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


	private class BuyRequestResponseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			AID sender = msg.getSender();

			if (msg != null) {
				// result message received. Process it
				try {
					FoodRequest foodRequest = (FoodRequest) msg.getContentObject();

					FoodResponse foodResponse = sellMatchingFood(foodRequest);

					ACLMessage message;

					if (foodResponse == null) {
						message = new ACLMessage(ACLMessage.DISCONFIRM);
					} else {
						message = new ACLMessage(ACLMessage.CONFIRM);
						message.setContentObject(foodResponse);
					}

					message.addReceiver(sender);
					send(message);

				} catch (UnreadableException | IOException e) {
					e.printStackTrace();
				}
			} else {
				block();
			}
		}

		private FoodResponse sellMatchingFood(FoodRequest foodRequest) {
			FoodResponse foodResponse = null;

			for (Food food : producedFood) {
				if (food.getFoodType() == foodRequest.getFoodType()) {
					if(foodRequest.getBuyingStrategy() == Constants.BuyingStrategy.price) {
						if(food.getPrice() <= foodRequest.getMaxPrice()) {
							foodResponse = new FoodResponse(food.getFoodType(), food.getPrice(), food.getQuality());
							moneyBalance += food.getPrice();
							producedFood.remove(food);
							break;
						}
					} else if(foodRequest.getBuyingStrategy() == Constants.BuyingStrategy.quality) {
						if (food.getQuality() >= foodRequest.getMinQuality()) {
							foodResponse = new FoodResponse(food.getFoodType(), food.getPrice(), food.getQuality());
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
