package com.formation.hibernate.entity;

import jakarta.persistence.*;
import java.util.List;

/**
 * 🎓 DEPARTMENT - Entidade Simples para Demonstração N+1
 * 
 * Esta entidade foi SIMPLIFICADA ao máximo para focar no problema N+1:
 * ✅ Apenas campos essenciais (id, name)
 * ✅ Relacionamento básico com User (@OneToMany)
 * ✅ Configuração mínima necessária
 * 
 * 🎯 FOCO: Demonstrar como Department é carregado quando acessamos user.getDepartment()
 */

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // ✅ Relacionamento simples para demonstrar N+1
    // LAZY por padrão (bom para demonstração)
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<User> users;

    // Construtores
    public Department() {}

    public Department(String name) {
        this.name = name;
    }

    // Getters e Setters básicos
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Department{id=" + id + ", name='" + name + "'}";
    }
}