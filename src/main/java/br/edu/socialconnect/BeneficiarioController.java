package br.edu.socialconnect;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/beneficiarios")
public class BeneficiarioController {
    private final Map<Long, Beneficiario> beneficiarios = new ConcurrentHashMap<>(Map.of(
            1L, new Beneficiario(1L, "Ana Souza"),
            2L, new Beneficiario(2L, "Bruno Lima")
    ));

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<Beneficiario> listar() {
        return beneficiarios.values().stream().sorted(Comparator.comparingLong(Beneficiario::id)).toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        if (beneficiarios.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    public record Beneficiario(long id, String nome) {}
}
