/* ==================================================================
 * NodeCertificatesController.java - Dec 7, 2012 7:16:19 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.setup.web;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.node.setup.PKIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for node certificate management.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/certs")
public class NodeCertificatesController extends BaseSetupController {

	@Autowired
	private PKIService pkiService;

	@RequestMapping
	public String home(Model model) {
		X509Certificate nodeCert = pkiService.getNodeCertificate();
		final Date now = new Date();
		final boolean expired = (nodeCert != null && now.after(nodeCert.getNotAfter()));
		final boolean valid = (!nodeCert.getIssuerDN().equals(nodeCert.getSubjectDN())
				&& !now.before(nodeCert.getNotBefore()) && !expired);
		model.addAttribute("nodeCert", nodeCert);
		model.addAttribute("nodeCertExpired", expired);
		model.addAttribute("nodeCertValid", valid);
		return "certs/home";
	}

	@RequestMapping("/nodeCSR")
	@ResponseBody
	public Map<String, Object> nodeCSR() {
		String csr = pkiService.generateNodePKCS10CertificateRequestString();
		Map<String, Object> result = new HashMap<String, Object>(1);
		result.put("csr", csr);
		return result;
	}
}