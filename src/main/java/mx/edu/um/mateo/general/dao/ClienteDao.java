/*
 * The MIT License
 *
 * Copyright 2012 J. David Mendoza <jdmendoza@um.edu.mx>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package mx.edu.um.mateo.general.dao;

import java.util.HashMap;
import java.util.Map;
import mx.edu.um.mateo.general.model.Cliente;
import mx.edu.um.mateo.general.model.TipoCliente;
import mx.edu.um.mateo.general.model.Usuario;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author J. David Mendoza <jdmendoza@um.edu.mx>
 */
@Repository
@Transactional
public class ClienteDao {

    private static final Logger log = LoggerFactory.getLogger(ClienteDao.class);
    @Autowired
    private SessionFactory sessionFactory;

    public ClienteDao() {
        log.info("Se ha creado una nueva instancia de ClienteDao");
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    public Map<String, Object> lista(Map<String, Object> params) {
        log.debug("Buscando lista de clientes con params {}", params);
        if (params == null) {
            params = new HashMap<>();
        }

        if (!params.containsKey("max")) {
            params.put("max", 10);
        } else {
            params.put("max", Math.min((Integer) params.get("max"), 100));
        }

        if (params.containsKey("pagina")) {
            Long pagina = (Long) params.get("pagina");
            Long offset = (pagina - 1) * (Integer) params.get("max");
            params.put("offset", offset.intValue());
        }

        if (!params.containsKey("offset")) {
            params.put("offset", 0);
        }
        Criteria criteria = currentSession().createCriteria(Cliente.class);
        Criteria countCriteria = currentSession().createCriteria(Cliente.class);

        if (params.containsKey("empresa")) {
            criteria.createCriteria("empresa").add(Restrictions.idEq(params.get("empresa")));
            countCriteria.createCriteria("empresa").add(Restrictions.idEq(params.get("empresa")));
        }

        if (params.containsKey("tipoCliente")) {
            criteria.createCriteria("tipoCliente").add(Restrictions.idEq(params.get("tipoCliente")));
            countCriteria.createCriteria("tipoCliente").add(Restrictions.idEq(params.get("tipoCliente")));
        }

        if (params.containsKey("filtro")) {
            String filtro = (String) params.get("filtro");
            filtro = "%" + filtro + "%";
            Disjunction propiedades = Restrictions.disjunction();
            propiedades.add(Restrictions.ilike("nombre", filtro));
            propiedades.add(Restrictions.ilike("nombreCompleto", filtro));
            propiedades.add(Restrictions.ilike("rfc", filtro));
            propiedades.add(Restrictions.ilike("correo", filtro));
            propiedades.add(Restrictions.ilike("contacto", filtro));
            criteria.add(propiedades);
            countCriteria.add(propiedades);
        }

        if (params.containsKey("order")) {
            String campo = (String) params.get("order");
            if (params.get("sort").equals("desc")) {
                criteria.addOrder(Order.desc(campo));
            } else {
                criteria.addOrder(Order.asc(campo));
            }
        }

        if (!params.containsKey("reporte")) {
            criteria.setFirstResult((Integer) params.get("offset"));
            criteria.setMaxResults((Integer) params.get("max"));
        }
        params.put("clientes", criteria.list());

        countCriteria.setProjection(Projections.rowCount());
        params.put("cantidad", (Long) countCriteria.list().get(0));

        return params;
    }

    public Cliente obtiene(Long id) {
        Cliente cliente = (Cliente) currentSession().get(Cliente.class, id);
        return cliente;
    }

    public Cliente crea(Cliente cliente, Usuario usuario) {
        Session session = currentSession();
        if (usuario != null) {
            cliente.setEmpresa(usuario.getEmpresa());
        }
        cliente.setTipoCliente((TipoCliente)session.get(TipoCliente.class, cliente.getTipoCliente().getId()));
        session.save(cliente);
        session.flush();
        return cliente;
    }

    public Cliente crea(Cliente cliente) {
        return this.crea(cliente, null);
    }

    public Cliente actualiza(Cliente cliente) {
        return this.actualiza(cliente, null);
    }

    public Cliente actualiza(Cliente cliente, Usuario usuario) {
        Session session = currentSession();
        if (usuario != null) {
            cliente.setEmpresa(usuario.getEmpresa());
        }
        cliente.setTipoCliente((TipoCliente)session.get(TipoCliente.class, cliente.getTipoCliente().getId()));
        session.update(cliente);
        session.flush();
        return cliente;
    }

    public String elimina(Long id) {
        Cliente cliente = obtiene(id);
        String nombre = cliente.getNombre();
        currentSession().delete(cliente);
        currentSession().flush();
        return nombre;
    }
}
