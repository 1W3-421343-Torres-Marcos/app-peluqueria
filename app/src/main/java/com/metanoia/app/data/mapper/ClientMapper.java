package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientOrigin;
import com.metanoia.domain.model.ClientSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte entre entidades/proyecciones Room de clientes y modelos de dominio.
 */
public final class ClientMapper {

    private ClientMapper() {
    }

    public static Client toDomain(ClientEntity entity) {
        return new Client.Builder()
                .id(entity.id)
                .name(entity.name)
                .nameNormalized(entity.nameNormalized)
                .phone(entity.phone)
                .birthDay(entity.birthDay)
                .birthMonth(entity.birthMonth)
                .birthYear(entity.birthYear)
                .notes(entity.notes)
                .noShowCount(entity.noShowCount)
                .contactLookupKey(entity.contactLookupKey)
                .origin(ClientOrigin.fromStorage(entity.origin))
                .createdFromContactAt(entity.createdFromContactAt)
                .active(entity.active)
                .createdAt(entity.createdAt)
                .updatedAt(entity.updatedAt)
                .build();
    }

    public static ClientEntity toEntity(Client client) {
        return new ClientEntity(
                client.id(),
                client.name(),
                client.nameNormalized(),
                client.phone(),
                client.birthDay().orElse(null),
                client.birthMonth().orElse(null),
                client.birthYear().orElse(null),
                client.notes(),
                client.noShowCount(),
                client.isActive(),
                client.createdAt(),
                client.updatedAt(),
                client.contactLookupKey().orElse(null),
                client.origin().name(),
                client.createdFromContactAt().orElse(null)
        );
    }

    public static List<ClientSummary> toSummaryList(List<ClientRowProjection> rows) {
        List<ClientSummary> out = new ArrayList<>(rows.size());
        for (ClientRowProjection row : rows) {
            out.add(new ClientSummary(
                    row.id,
                    row.name,
                    row.phone,
                    row.noShowCount,
                    row.active
            ));
        }
        return out;
    }
}
