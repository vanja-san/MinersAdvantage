package uk.co.duelmonster.minersadvantage.workers;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.Tags;
import uk.co.duelmonster.minersadvantage.common.Constants;
import uk.co.duelmonster.minersadvantage.common.Functions;
import uk.co.duelmonster.minersadvantage.config.MAConfig;
import uk.co.duelmonster.minersadvantage.helpers.BreakBlockController;
import uk.co.duelmonster.minersadvantage.network.packets.PacketShaftanate;

public class ShaftanationAgent extends Agent {
	
	public ShaftanationAgent(ServerPlayerEntity player, PacketShaftanate pkt) {
		super(player, pkt);
		
		this.originPos = pkt.pos;
		this.faceHit = pkt.faceHit;
		this.originState = Block.getStateById(pkt.stateID);
		
		if (originState == null || originState.getBlock() == Blocks.AIR)
			Constants.LOGGER.log(Level.INFO, "Invalid BlockState ID recieved from message packet. [ " + pkt.stateID + " ]");
		
		this.originBlock = originState.getBlock();
		
		this.shouldAutoIlluminate = MAConfig.CLIENT.common.autoIlluminate();
		
		setupShaft();
		
		addConnectedToQueue(originPos);
	}
	
	// Returns true when Shaftanation is complete or cancelled
	@Override
	public boolean tick() {
		if (originPos == null || player == null || !player.isAlive() || processed.size() >= MAConfig.CLIENT.common.blockLimit())
			return true;
		
		boolean bIsComplete = false;
		
		for (int iQueueCount = 0; queued.size() > 0; iQueueCount++) {
			if ((MAConfig.CLIENT.common.breakAtToolSpeeds() && iQueueCount > 0)
					|| iQueueCount >= MAConfig.CLIENT.common.blocksPerTick()
					|| processed.size() >= MAConfig.CLIENT.common.blockLimit()
					|| (!MAConfig.CLIENT.common.breakAtToolSpeeds() && MAConfig.CLIENT.common.tpsGuard() && timer.elapsed(TimeUnit.MILLISECONDS) > 40))
				break;
			
			if (Functions.IsPlayerStarving(player)) {
				bIsComplete = true;
				break;
			}
			
			BlockPos oPos = queued.remove(0);
			if (oPos == null || !Functions.isWithinArea(oPos, harvestArea) || world.getBlockState(oPos).getBlock() == Blocks.TORCH)
				continue;
			
			BlockState	state	= world.getBlockState(oPos);
			Block		block	= state.getBlock();
			
			if (!fakePlayer().canHarvestBlock(state)) {
				// Avoid the non-harvestable blocks.
				processed.add(oPos);
				continue;
			}
			
			// Process the current block if it is valid.
			if (MAConfig.CLIENT.common.mineVeins() && state.isIn(Tags.Blocks.ORES)) {
				processed.add(oPos);
				excavateOreVein(state, oPos);
			} else {
				world.captureBlockSnapshots = true;
				world.capturedBlockSnapshots.clear();
				
				boolean bBlockHarvested = false;
				
				if (MAConfig.CLIENT.common.breakAtToolSpeeds()) {
					this.breakController = new BreakBlockController(fakePlayer());
					breakController.onPlayerDamageBlock(oPos, faceHit);
					if (breakController.bBlockDestroyed)
						bBlockHarvested = HarvestBlock(oPos);
					
				} else bBlockHarvested = HarvestBlock(oPos);
				
				if (bBlockHarvested) {
					processBlockSnapshots();
					
					SoundType soundtype = block.getSoundType(state, world, oPos, null);
					reportProgessToClient(oPos, soundtype.getBreakSound());
					
					autoIlluminate((new BlockPos(oPos.getX(), feetPos, oPos.getZ())).offset(faceHit.getOpposite()), MAConfig.CLIENT.shaftanation.torchPlacement());
					
					addConnectedToQueue(oPos);
					
					processed.add(oPos);
				}
			}
		}
		
		return (bIsComplete || queued.isEmpty());
	}
	
	private void setupShaft() {
		// Shaft area info
		int	xStart	= 0;
		int	xEnd	= 0;
		int	yBottom	= feetPos;
		int	yTop	= feetPos + (MAConfig.CLIENT.shaftanation.shaftHeight() - 1);
		int	zStart	= 0;
		int	zEnd	= 0;
		
		// if the ShaftWidth is divisible by 2 we don't want to do anything
		double dDivision = ((MAConfig.CLIENT.shaftanation.shaftWidth() & 1) != 0 ? 0 : 0.5);
		
		switch (faceHit) {
		case SOUTH: // Positive Z
			xStart = originPos.getX() + ((int) ((MAConfig.CLIENT.shaftanation.shaftWidth() / 2) - dDivision));
			xEnd = originPos.getX() - (MAConfig.CLIENT.shaftanation.shaftWidth() / 2);
			zStart = originPos.getZ();
			zEnd = originPos.getZ() - MAConfig.CLIENT.shaftanation.shaftLength();
			break;
		case NORTH: // Negative Z
			xStart = originPos.getX() - (MAConfig.CLIENT.shaftanation.shaftWidth() / 2);
			xEnd = originPos.getX() + ((int) ((MAConfig.CLIENT.shaftanation.shaftWidth() / 2) - dDivision));
			zStart = originPos.getZ();
			zEnd = originPos.getZ() + MAConfig.CLIENT.shaftanation.shaftLength();
			break;
		case EAST: // Positive X
			xStart = originPos.getX();
			xEnd = originPos.getX() - MAConfig.CLIENT.shaftanation.shaftLength();
			zStart = originPos.getZ() + ((int) ((MAConfig.CLIENT.shaftanation.shaftWidth() / 2) - dDivision));
			zEnd = originPos.getZ() - (MAConfig.CLIENT.shaftanation.shaftWidth() / 2);
			break;
		case WEST: // Negative X
			xStart = originPos.getX();
			xEnd = originPos.getX() + MAConfig.CLIENT.shaftanation.shaftLength();
			zStart = originPos.getZ() - (MAConfig.CLIENT.shaftanation.shaftWidth() / 2);
			zEnd = originPos.getZ() + ((int) ((MAConfig.CLIENT.shaftanation.shaftWidth() / 2) - dDivision));
			break;
		default:
			break;
		}
		
		harvestArea = new AxisAlignedBB(
				xStart, yBottom, zStart,
				xEnd, yTop, zEnd);
		
	}
	
	@Override
	public void addToQueue(BlockPos oPos) {
		BlockState state = world.getBlockState(oPos);
		
		if (fakePlayer().canHarvestBlock(state))
			super.addToQueue(oPos);
	}
	
}
