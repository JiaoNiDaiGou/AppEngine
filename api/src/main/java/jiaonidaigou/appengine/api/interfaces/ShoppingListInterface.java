package jiaonidaigou.appengine.api.interfaces;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import jiaonidaigou.appengine.api.access.db.ShoppingListDbCilent;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaoni.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.wiremodel.api.AssignOwnershipShoppingListItemRequest;
import jiaonidaigou.appengine.wiremodel.api.ExpireShoppingListItemRequest;
import jiaonidaigou.appengine.wiremodel.api.InitShoppingListItemRequest;
import jiaonidaigou.appengine.wiremodel.api.PurchaseShoppingListItemRequest;
import jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem;
import jiaonidaigou.appengine.wiremodel.entity.ShoppingListItemOrBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem.Status.EXPIRED;
import static jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem.Status.INIT;
import static jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem.Status.IN_HOUSE;
import static jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem.Status.OWNERSHIP_ASSIGNED;
import static jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem.Status.PURCHASED;
import static jiaonidaigou.appengine.wiremodel.entity.ShoppingListItem.Status.UNKNOWN;

@Path("/api/shoppingLists")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ShoppingListInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListInterface.class);

    private static final Map<ShoppingListItem.Status, Set<ShoppingListItem.Status>> ALLOWED_STATUS_TRANFORM =
            ImmutableMap.<ShoppingListItem.Status, Set<ShoppingListItem.Status>>builder()
                    .put(UNKNOWN, Sets.newHashSet(ShoppingListItem.Status.values()))
                    .put(INIT, Sets.newHashSet(INIT, OWNERSHIP_ASSIGNED, PURCHASED, EXPIRED))
                    .put(OWNERSHIP_ASSIGNED, Sets.newHashSet(OWNERSHIP_ASSIGNED, PURCHASED, EXPIRED))
                    .put(PURCHASED, Sets.newHashSet(PURCHASED, IN_HOUSE))
                    .put(EXPIRED, Sets.newHashSet(EXPIRED, INIT))
                    .put(IN_HOUSE, Sets.newHashSet(IN_HOUSE))
                    .build();

    private final ShoppingListDbCilent dbCilent;

    @Inject
    public ShoppingListInterface(final ShoppingListDbCilent dbCilent) {
        this.dbCilent = dbCilent;
    }

    private static Set<String> mergeMediaIds(final ShoppingListItemOrBuilder items,
                                             final Collection<String> newMediaIds) {
        return ImmutableSet.<String>builder()
                .addAll(items.getMediaIdsList())
                .addAll(newMediaIds)
                .build();
    }

    @GET
    @Path("/get/{id}")
    public Response getById(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);
        ShoppingListItem item = dbCilent.getById(id);
        if (item == null) {
            throw new NotFoundException();
        }
        return Response.ok(item).build();
    }

    @POST
    @Path("/init")
    public Response init(final InitShoppingListItemRequest request) {
        RequestValidator.validateNotBlank(request.getCreatorName());
        RequestValidator.validateRequest(StringUtils.isNotBlank(request.getMessage()) ||
                request.getProductEntriesCount() > 0);

        ShoppingListItem toSave = ShoppingListItem.newBuilder()
                .setStatus(ShoppingListItem.Status.INIT)
                .setCreationTime(System.currentTimeMillis())
                .setCreatorName(request.getCreatorName())
                .setExpirationTime(-1)
                .setMessage(request.getMessage())
                .addAllProductEntries(request.getProductEntriesList())
                .addAllMediaIds(request.getMediaIdsList())
                .setLastUpdateTime(System.currentTimeMillis())
                .build();

        ShoppingListItem afterSave = dbCilent.put(toSave);
        LOGGER.info("Init shoppingListItem. id={}, creationName={}. item={}",
                afterSave.getId(),
                afterSave.getCreatorName(),
                ObjectMapperProvider.compactToJson(afterSave)
        );

        return Response.ok(afterSave).build();
    }

    @POST
    @Path("/{id}/assign")
    public Response assignOwnership(@PathParam("id") final String id,
                                    final AssignOwnershipShoppingListItemRequest request) {
        RequestValidator.validateNotBlank(request.getOwnerName());

        ShoppingListItem toReturn = update(id, OWNERSHIP_ASSIGNED, t ->
                t.setOwnerName(request.getOwnerName())
                        .setOwnershipAssignedTime(System.currentTimeMillis())
        );

        return Response.ok(toReturn).build();
    }

    @POST
    @Path("/{id}/purchase")
    public Response purchase(@PathParam("id") final String id,
                             final PurchaseShoppingListItemRequest request) {
        RequestValidator.validateNotBlank(request.getPurchaserName());
        RequestValidator.validateNotNull(request.getTotalPurchasePrice());

        ShoppingListItem toSave = update(id, PURCHASED, t -> {
            if (StringUtils.isNotBlank(t.getOwnerName())) {
                t.setOwnerName(request.getPurchaserName());
            }
            long now = System.currentTimeMillis();
            if (t.getOwnershipAssignedTime() == 0L) {
                t.setOwnershipAssignedTime(now);
            }
            Set<String> mediaIds = mergeMediaIds(t, request.getMediaIdsList());
            t.setStatus(PURCHASED)
                    .setPurchaserName(request.getPurchaserName())
                    .setPurchasingTime(now)
                    .setPurchasingSource(request.getPurchasingSource())
                    .clearMediaIds()
                    .addAllMediaIds(mediaIds)
                    .setTotalPurchasePrice(request.getTotalPurchasePrice())
                    .build();
        });
        return Response.ok(toSave).build();
    }

    @POST
    @Path("/{id}/inHouse")
    public Response inHouse(@PathParam("id") final String id) {
        ShoppingListItem toReturn = update(id, IN_HOUSE, t ->
                t.setInHouseTime(System.currentTimeMillis())
        );
        return Response.ok(toReturn).build();
    }

    @POST
    @Path("/{id}/expire")
    public Response expire(@PathParam("id") final String id,
                           final ExpireShoppingListItemRequest request) {
        RequestValidator.validateNotBlank(request.getExpireName());

        update(id, EXPIRED, t -> {
            long expireTime = request.getExpirationTime() == 0L ? System.currentTimeMillis() : request.getExpirationTime();
            t.setExpirationTime(expireTime)
                    .setExpireName(request.getExpireName());
        });
        return Response.ok().build();
    }

    @GET
    @Path("/query")
    public Response query(@QueryParam("status") final ShoppingListItem.Status status,
                          @QueryParam("onlyActive") final boolean onlyActive) {
        Stream<ShoppingListItem> itemsStream;
        if (status != null) {
            itemsStream = dbCilent.queryByStatus(status);
        } else if (onlyActive) {
            itemsStream = dbCilent.queryActive();
        } else {
            throw new UnsupportedOperationException();
        }
        List<ShoppingListItem> items = itemsStream
                .sorted((a, b) -> Long.compare(b.getLastUpdateTime(), a.getLastUpdateTime()))
                .collect(Collectors.toList());
        return Response.ok(items).build();
    }

    private ShoppingListItem update(final String id,
                                    final ShoppingListItem.Status toStatus,
                                    final Consumer<ShoppingListItem.Builder> enhancer) {
        RequestValidator.validateNotBlank(id, "shoppingListId");
        ShoppingListItem item = dbCilent.getById(id);
        if (item == null) {
            throw new NotFoundException();
        }
        ShoppingListItem.Status fromStatus = item.getStatus();
        if (!canUpdateStatus(item.getStatus(), toStatus)) {
            LOGGER.error("Cannot update status from {} to {}.", fromStatus, toStatus);
            throw new BadRequestException();
        }

        ShoppingListItem.Builder builder = item.toBuilder()
                .setStatus(toStatus)
                .setLastUpdateTime(System.currentTimeMillis());
        enhancer.accept(builder);
        ShoppingListItem toReturn = dbCilent.put(builder.build());
        LOGGER.info("Update shoppingListItem. id={}. fromStatus={}, toStatus={}. from={}. to={}",
                id,
                fromStatus,
                toStatus,
                ObjectMapperProvider.compactToJson(item),
                ObjectMapperProvider.compactToJson(toReturn));
        return toReturn;
    }

    private boolean canUpdateStatus(final ShoppingListItem.Status from,
                                    final ShoppingListItem.Status to) {
        Set<ShoppingListItem.Status> allowedToStatuses = ALLOWED_STATUS_TRANFORM.get(from);
        return allowedToStatuses != null && allowedToStatuses.contains(to);
    }
}
