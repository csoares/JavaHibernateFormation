package com.formation.hibernate.service;

import com.formation.hibernate.entity.Department;
import com.formation.hibernate.entity.User;
import com.formation.hibernate.repository.DepartmentRepository;
import com.formation.hibernate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 🎓 DATA SERVICE - Serviço ULTRA SIMPLES para Demonstração N+1
 * 
 * Serviço mínimo focado apenas em popular dados para demonstrar N+1:
 * ✅ Apenas criação de Departments e Users
 * ✅ Dados mínimos necessários
 * ✅ Sem complexidades desnecessárias
 * 
 * 🎯 FOCO: Criar dados suficientes para ver o problema N+1
 */

@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DataService(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void populateData() {
        // Limpar dados existentes
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        logger.info("🗑️ Dados limpos. Criando dados para demonstração N+1...");

        // Criar departamentos simples
        Department tech = departmentRepository.save(new Department("Tecnologia"));
        Department sales = departmentRepository.save(new Department("Vendas"));
        Department hr = departmentRepository.save(new Department("Recursos Humanos"));

        logger.info("✅ Criados 3 departamentos");

        // Criar users simples
        userRepository.save(new User("João Silva", "joao@tech.com", tech));
        userRepository.save(new User("Maria Santos", "maria@tech.com", tech));
        userRepository.save(new User("Pedro Costa", "pedro@sales.com", sales));
        userRepository.save(new User("Ana Lima", "ana@sales.com", sales));
        userRepository.save(new User("Carlos Mendes", "carlos@hr.com", hr));
        userRepository.save(new User("Sofia Pereira", "sofia@hr.com", hr));
        
        // Alguns users sem departamento para variedade
        userRepository.save(new User("Bruno Alves", "bruno@freelance.com"));
        userRepository.save(new User("Rita Ferreira", "rita@freelance.com"));

        logger.info("✅ Criados 8 users");
        logger.info("🎯 Dados prontos para demonstração N+1!");
    }

    @Transactional(readOnly = true)
    public void printStatistics() {
        long departmentCount = departmentRepository.count();
        long userCount = userRepository.count();
        
        logger.info("📊 ESTATÍSTICAS SIMPLES:");
        logger.info("   Departments: {}", departmentCount);
        logger.info("   Users: {}", userCount);
    }

    @Transactional
    public void clearData() {
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        logger.info("🗑️ Todos os dados foram removidos");
    }
}