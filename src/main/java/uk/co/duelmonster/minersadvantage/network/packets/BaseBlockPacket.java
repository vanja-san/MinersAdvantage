package uk.co.duelmonster.minersadvantage.network.packets;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public abstract class BaseBlockPacket implements IMAPacket {

	public final BlockPos pos;
	public final Direction faceHit;
	public final int stateID;

	public BaseBlockPacket(BlockPos _pos, Direction _faceHit) {
		pos = _pos;
		faceHit = _faceHit;
		stateID = 0;
	}
	public BaseBlockPacket(BlockPos _pos, Direction _faceHit, int _stateID) {
		pos = _pos;
		faceHit = _faceHit;
		stateID = _stateID;
	}
	public BaseBlockPacket(PacketBuffer buf) {
		pos = buf.readBlockPos();
		faceHit = buf.readEnumValue(Direction.class);
		stateID = buf.readInt();
	}
}
