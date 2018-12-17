package ultimatumGame;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerAgent extends Agent {
	// the strategy the agent will follow
	private String strategy = "PROFIT"; // EQUALITY || PROFIT
	// amount of money in posession
	private int money = 0;
	// count of received spread requests
	private int spreadRequestCount = 0;
	// count of received share requests
	private int shareRequestCount = 0;
	// the amount to negotiate with this round
	private int amountToPlay = 0;
	// the amount to negotiate with this round
	private int winningThreshold = 10000;
	// a list of known player agents
	private AID[] playerAgents;
	// the bank agent that submitted the spread request
	private AID bankAgent;


	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Player Agent " + getAID().getName() + " is ready.");

		// get strategy argument if specified, else use default strategy
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			strategy = (String) args[0];
			if(!strategy.equals("EQUALITY")){
				strategy = "PROFIT";
			}

			if (args.length > 1) {
				winningThreshold = Integer.parseInt((String) args[1]);
			}
		}
		System.out.println("Chosen strategy is " + strategy + ".");


		// Register the player agent service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("player-service");
		sd.setName("JADE-ultimatum-game");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
			System.out.println("Registered " + getAID().getName() + "  as player agent.");
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// add bank request behaviour
		addBehaviour(new BankRequestServer());

		// add share request behaviour
		addBehaviour(new ShareRequestServer());

		// add share request behaviour
		addBehaviour(new AgentDeletedServer());
		
	}

	// agent takedown operations here, deregister agent service
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("Player Agent " + getAID().getName() + " terminating now. His stash was " + money + ", he received " + spreadRequestCount + " spread requests and " + shareRequestCount + " share requests");
	}

	private class BankRequestServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP message received. Process it
				String amount = msg.getContent();
				bankAgent = msg.getSender();
				amountToPlay = Integer.parseInt(amount);
				spreadRequestCount ++;
				System.out.println("Received money spread request #" + spreadRequestCount +" for " + amountToPlay);

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
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				myAgent.addBehaviour(new RoundPerformer());
			} else {
				block();
			}
		}
	}

	private class ShareRequestServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// share proposal message received. Process it
				ArrayList<Integer> content = new ArrayList<Integer>();
				try {
					content = (ArrayList<Integer>) msg.getContentObject();
				} catch (UnreadableException e){
					e.printStackTrace();
				}
				int wholeAmount = content.get(0);
				int share = content.get(1);
				shareRequestCount ++;
				System.out.println("Received share offer #" + shareRequestCount + " of " + share + " out of " + wholeAmount + " (" + String.format("%.2f", ((double) share / (double) wholeAmount * 100)) + "%).");
				ACLMessage reply = msg.createReply();

				if(isShareOkay(share, wholeAmount)){
					money += share;
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					System.out.println("Y - Accepted share of " + String.format("%.2f", ((double) share / (double) wholeAmount * 100)) + "%.");
					if(money > winningThreshold){
						doDelete();
					}
				} else{
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					System.out.println("N - Rejected share of " + String.format("%.2f", ((double) share / (double) wholeAmount * 100)) + "%.");
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}

		private boolean isShareOkay(int share, int wholeAmount){
			// TODO: implement some rules
			return (int) Math.round(Math.random()) != 0;
		}
	}

	private class AgentDeletedServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				doDelete();
			} else {
				block();
			}
		}
	}

	private class RoundPerformer extends Behaviour {
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private int shareToOffer = 0;
		private int wholeAmount = amountToPlay;
		private AID localBank = bankAgent;

		public void action() {
			switch (step) {
			case 0:
				// send the proposal with the share amount to one agent
				ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
				cfp.addReceiver(selectReceiver());
				ArrayList<Integer> content = new ArrayList<Integer>();
				content.add(wholeAmount); //add amount to play in first place
				shareToOffer = computeShareToOffer(wholeAmount);
				content.add(shareToOffer); // add share for other player in second place
				try {
					cfp.setContentObject(content);
				} catch (IOException e){
					e.printStackTrace();
				}
				cfp.setConversationId("process-round");
				cfp.setReplyWith(getAID().getName() + System.currentTimeMillis());
				myAgent.send(cfp); // send share offer

				System.out.println("Offered a share of " + shareToOffer + " out of " + wholeAmount + " (" + String.format("%.2f", ((double) shareToOffer / (double) wholeAmount * 100)) + "%).");

				// prepare message template for receiving a response
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("process-round"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive response for proposed share
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// reply received
					if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
						// peer has accepted the share, add our share to the money stack
						money += (wholeAmount - shareToOffer);
						System.out.println("Y - Player accepted the share of " + String.format("%.2f", ((double) shareToOffer / (double) amountToPlay * 100)) + "%.");
						sendReply(String.valueOf(wholeAmount));
						if(money > winningThreshold){
							doDelete();
						}
					} else{
						sendReply("N");
						System.out.println("N - Player did not accept a share of " + String.format("%.2f", ((double) shareToOffer / (double) amountToPlay * 100)) + "%.");
					}
					step = 2;
				} else {
					block();
				}
				break;
			}
		}

		private int computeShareToOffer(int wholeAmount){
			// TODO: implement some rules
			return (int) ((Math.random() / 2) * wholeAmount);
		}

		private void sendReply(String result){
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			cfp.addReceiver(localBank);
			cfp.setContent(result);
			myAgent.send(cfp);
			System.out.println("Negotiation response to bank agent sent.");
		}

		private AID selectReceiver(){
			AID selectedReceiver = getAID();
			while (selectedReceiver == getAID()) {
				int randomReceiver = (int) (Math.random() * playerAgents.length); // random int between 0 and size of playerAgents - 1
				selectedReceiver = playerAgents[randomReceiver];
			}
			return selectedReceiver;
		}

		public boolean done() {
			return step == 2;
		}
	}
}
