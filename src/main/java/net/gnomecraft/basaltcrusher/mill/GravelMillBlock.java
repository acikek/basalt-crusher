package net.gnomecraft.basaltcrusher.mill;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.gnomecraft.basaltcrusher.BasaltCrusher;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GravelMillBlock extends BlockWithEntity {
    static final IntProperty MILL_STATE = IntProperty.of("mill_state",0, 21);
    static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public GravelMillBlock(Settings settings) {
        super(settings);

        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(MILL_STATE, 21));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GravelMillEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, BasaltCrusher.GRAVEL_MILL_ENTITY, GravelMillEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            this.openContainer(world, pos, player);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof GravelMillEntity) {
            ((GravelMillEntity) blockEntity).dropExperience(player);
        }

        super.onBreak(world, pos, state, player);
    }

    private void openContainer(World world, BlockPos blockPos, PlayerEntity playerEntity) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity instanceof GravelMillEntity) {
            playerEntity.openHandledScreen((NamedScreenHandlerFactory) blockEntity);
            // TODO: playerEntity.increaseStat(Stats.INTERACT_WITH_MILL, 1);
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState blockState, LootContext.Builder lootContext$Builder) {
        ArrayList<ItemStack> dropList = new ArrayList<>();
        dropList.add(new ItemStack(this));
        return dropList;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(MILL_STATE) < 20) {
            double x = (double) pos.getX() + 0.5D;
            double y = (double) pos.getY();
            double z = (double) pos.getZ() + 0.5D;

            if (random.nextDouble() < 0.1D) {
                // TODO: Replace with a custom sound.
                world.playSound(x, y, z, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
            }
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getPlayerFacing().getOpposite());
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof GravelMillEntity) {
                ((GravelMillEntity) blockEntity).scatterInventory(world, pos);
                world.updateComparators(pos,this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);

        if (entity instanceof GravelMillEntity) {
            return ((GravelMillEntity) entity).calculateComparatorOutput();
        }

        return 0;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, MILL_STATE);
    }
}