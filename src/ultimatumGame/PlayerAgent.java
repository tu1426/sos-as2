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
	// amount offered with spreads
	private int spreadRequestAmount = 0;
	// count of received share requests
	private int shareRequestCount = 0;
	// amount offered with shares
	private int shareRequestAmount = 0;
	// the lowest percentage of a share that was accepted
	private double lowestShareAccepted = 100.0;
	// the highest percentage of a share that was rejected
	private double highestShareRejected = 0.0;
	// the minimum percentage of a share that received
	private double minShareReceived = 100.0;
	// the maximum percentage of a share that was received
	private double maxShareReceived = 0.0;
	// the amount to negotiate with this round
	private int amountToPlay = 0;
	// the amount to negotiate with this round
	private int winningThreshold = 10000;
	// a list of known player agents
	private AID[] playerAgents;
	// the bank agent that submitted the spread request
	private AID bankAgent;
	// flag for printing verbose log
	private boolean verbose;


	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		print("Player Agent " + getAID().getName() + " is ready.", true);

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

			if (args.length > 2) {
				verbose = Boolean.valueOf((String) args[2]);
			}
		}
		print("Chosen strategy is " + strategy + ", winningThreshold is " + winningThreshold + ", verbose is " + verbose + ".", true);


		// Register the player agent service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("player-service");
		sd.setName("JADE-ultimatum-game");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
			print("Registered " + getAID().getName() + "  as player agent.");
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

		print(String.format("%-15s%-15s%-15d%-10d%-15d%-10d%-20d", getAID().getLocalName(), strategy, money, spreadRequestCount, spreadRequestAmount, shareRequestCount, shareRequestAmount), true);
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
				spreadRequestAmount += amountToPlay;
				print("Received money spread request #" + spreadRequestCount +" for " + amountToPlay);

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
				shareRequestAmount += share;
				double sharePercentage = ((double) share / (double) wholeAmount * 100);
				if(sharePercentage > maxShareReceived){
					maxShareReceived = sharePercentage;
				}
				if(sharePercentage < minShareReceived){
					minShareReceived = sharePercentage;
				}
				print("Received share offer #" + shareRequestCount + " of " + share + " out of " + wholeAmount + " (" + String.format("%.2f", sharePercentage) + "%).");
				ACLMessage reply = msg.createReply();

				if(isShareOkay(share, wholeAmount)){
					money += share;
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					print("Y - Accepted share of " + String.format("%.2f", sharePercentage) + "%.");
					if(money > winningThreshold){
						doDelete();
					}
				} else{
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					print("N - Rejected share of " + String.format("%.2f", sharePercentage) + "%.");
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}

		private boolean isShareOkay(int share, int wholeAmount){
			if(money + share > winningThreshold){
				return true;
			}
			if(strategy.equals("EQUALITY")){
				if((double) share / (double) wholeAmount >= 0.4){
					return true;
				} else{
					print(String.format("strategy %s\nmoney %d\nshare %d\nwholeAmount %d\nsharePercentage %f\nminShareReceived %f\nmaxShareReceived %f\nspreadRequestAmount %d\nshareRequestAmount %d", strategy, money, share, wholeAmount, ((double) share / (double) wholeAmount), minShareReceived, maxShareReceived, spreadRequestAmount, shareRequestAmount), true);
					return false;
				}
			}
			if((double) money / (double) winningThreshold < 0.1 && (double) share / (double) wholeAmount * 100 >= Math.min(minShareReceived, 20)){
				return true;
			}
			if((double) money / (double) (spreadRequestAmount + shareRequestAmount) < 0.1 && (double) share / (double) wholeAmount >= 0.15){
				return true;
			}
			if((double) share / (double) wholeAmount * 100 >= Math.max(maxShareReceived - 1, 35)){
				return true;
			} else {
				print(String.format("strategy %s\nmoney %d\nshare %d\nwholeAmount %d\nsharePercentage %f\nminShareReceived %f\nmaxShareReceived %f\nspreadRequestAmount %d\nshareRequestAmount %d", strategy, money, share, wholeAmount, ((double) share / (double) wholeAmount), minShareReceived, maxShareReceived, spreadRequestAmount, shareRequestAmount), true);
				return false;
			}
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
			double sharePercentage = 0;
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

				sharePercentage = ((double) shareToOffer / (double) wholeAmount * 100);
				print("Offered a share of " + shareToOffer + " out of " + wholeAmount + " (" + String.format("%.2f", sharePercentage) + "%).");

				// prepare message template for receiving a response
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("process-round"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive response for proposed share
				sharePercentage = ((double) shareToOffer / (double) wholeAmount * 100);
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// reply received
					if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
						if(lowestShareAccepted > sharePercentage){
							lowestShareAccepted = sharePercentage;
						}
						money += (wholeAmount - shareToOffer);
						print("Y - Player accepted the share of " + String.format("%.2f", sharePercentage) + "%.");
						sendReply(String.valueOf(wholeAmount));
						if(money > winningThreshold){
							doDelete();
						}
					} else{
						if(highestShareRejected < sharePercentage){
							highestShareRejected = sharePercentage;
						}
						sendReply("N");
						print("N - Player did not accept a share of " + String.format("%.2f", sharePercentage) + "%.", true);
					}
					step = 2;
				} else {
					block();
				}
				break;
			}
		}

		private int computeShareToOffer(int wholeAmount){
			if(strategy.equals("EQUALITY")){
				return wholeAmount / 2;
			}
			if(money + (wholeAmount / 2) > winningThreshold){
				return wholeAmount - (winningThreshold - money);
			}
			if((double) money / (double) winningThreshold > 0.5){
				int shareMin = (int) Math.min(lowestShareAccepted, 20);
				int sharePercentage = (int) (Math.random() * (34 - shareMin)) + shareMin;
				return wholeAmount * sharePercentage / 100;
			}
			if(spreadRequestCount + shareRequestCount > 5 && money / (spreadRequestAmount + shareRequestAmount) < 0.15){
				int shareMax = (int) Math.max(highestShareRejected, 40);
				int sharePercentage = (int) (Math.random() * (shareMax - 33)) + 33;
				return wholeAmount * sharePercentage / 100;
			} else{
				int shareMin = (int) Math.min(lowestShareAccepted, 20);
				int shareMax = (int) Math.max(highestShareRejected, 40);
				int sharePercentage = (int) (Math.random() * (shareMax - shareMin)) + shareMin;
				return wholeAmount * sharePercentage / 100;
			}
		}

		private void sendReply(String result){
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			cfp.addReceiver(localBank);
			cfp.setContent(result);
			myAgent.send(cfp);
			print("Negotiation response to bank agent sent.");
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
